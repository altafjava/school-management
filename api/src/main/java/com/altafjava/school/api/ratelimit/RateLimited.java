package com.altafjava.school.api.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method (e.g. a CSV import or PDF generation) as expensive enough to need its
 * own stricter per-tenant limit on top of platform's flat request-count limit.
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
