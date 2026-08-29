package com.altafjava.school.api.ratelimit;

import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;

/**
 * Enforces {@link RateLimited} on top of platform's {@code RateLimitInterceptor} (a flat
 * per-tenant/per-user request-COUNT limit applied to every endpoint uniformly) — this is a
 * per-operation limit for the handful of endpoints where a single call is disproportionately
 * expensive. Reuses platform's own Redis-backed {@code ProxyManager<String>} bean, so limits are
 * enforced consistently across every app instance, not just the one that happened to serve a
 * given request.
 * <p>
 * Rejects with {@link BusinessException} (400) rather than a dedicated 429: this codebase's
 * shared {@code GlobalExceptionHandler} lives in platform-saas and has no generic
 * "too many requests" mapping, and adding a school-saas-local {@code @ControllerAdvice} just for
 * this would need careful ordering against platform's own catch-all handler for no real gain over
 * an already-clear rejection message.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

	private final ProxyManager<String> proxyManager;

	@Around("@annotation(rateLimited)")
	public Object enforce(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
		Long tenantId = TenantContext.getCurrentTenantId();
		String bucketKey = "school:ratelimit:" + rateLimited.key() + ":" + tenantId;

		Bandwidth bandwidth = Bandwidth.builder()
				.capacity(rateLimited.capacity())
				.refillGreedy(rateLimited.capacity(), Duration.ofMinutes(rateLimited.periodMinutes()))
				.build();
		BucketConfiguration configuration = BucketConfiguration.builder().addLimit(bandwidth).build();
		Bucket bucket = proxyManager.builder().build(bucketKey, () -> configuration);

		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (!probe.isConsumed()) {
			long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
			throw new BusinessException("Rate limit exceeded for this operation — try again in "
					+ waitSeconds + " seconds");
		}

		return joinPoint.proceed();
	}
}
