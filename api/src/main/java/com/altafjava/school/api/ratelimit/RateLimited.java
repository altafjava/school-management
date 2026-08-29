package com.altafjava.school.api.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as expensive enough to need its own, stricter, per-tenant limit on
 * top of platform's flat per-tenant/per-user request-count limit ({@code RateLimitInterceptor}) —
 * for endpoints where a single call (a CSV import, a PDF generation) costs far more CPU/DB time
 * than a typical request, so a tenant staying well under the general hourly quota can still
 * degrade latency for every other tenant on the same instance by hammering just this one.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

	/** Bucket identity — unique per protected operation, combined with the tenant ID. */
	String key();

	/** Max calls allowed per tenant within {@link #periodMinutes()}. */
	int capacity();

	int periodMinutes();
}
