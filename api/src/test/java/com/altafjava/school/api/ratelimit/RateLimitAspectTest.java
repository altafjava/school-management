package com.altafjava.school.api.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;

class RateLimitAspectTest {

	@SuppressWarnings("unchecked")
	private final ProxyManager<String> proxyManager = mock(ProxyManager.class);
	@SuppressWarnings("unchecked")
	private final RemoteBucketBuilder<String> bucketBuilder = mock(RemoteBucketBuilder.class);
	private final BucketProxy bucket = mock(BucketProxy.class);
	private final ConsumptionProbe probe = mock(ConsumptionProbe.class);
	private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

	private RateLimitAspect aspect;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		aspect = new RateLimitAspect(proxyManager);
		when(proxyManager.builder()).thenReturn(bucketBuilder);
		when(bucketBuilder.build(anyString(), (Supplier<BucketConfiguration>) org.mockito.ArgumentMatchers.any()))
				.thenReturn(bucket);
		when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	private RateLimited rateLimitedAnnotation() throws NoSuchMethodException {
		Method method = SampleAnnotated.class.getMethod("limited");
		return method.getAnnotation(RateLimited.class);
	}

	@Test
	void withinLimit_proceedsToJoinPoint() throws Throwable {
		when(probe.isConsumed()).thenReturn(true);
		when(joinPoint.proceed()).thenReturn("ok");

		Object result = aspect.enforce(joinPoint, rateLimitedAnnotation());

		assertEquals("ok", result);
	}

	@Test
	void overLimit_throwsBusinessException_andNeverProceeds() throws Throwable {
		when(probe.isConsumed()).thenReturn(false);
		when(probe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L);

		assertThrows(BusinessException.class, () -> aspect.enforce(joinPoint, rateLimitedAnnotation()));

		org.mockito.Mockito.verify(joinPoint, org.mockito.Mockito.never()).proceed();
	}

	private interface SampleAnnotated {
		@RateLimited(key = "sample", capacity = 5, periodMinutes = 60)
		void limited();
	}
}
