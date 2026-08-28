package com.altafjava.school.domain.metrics;

import java.util.Set;

/**
 * School-specific metric type keys, registered with the platform's open metric-type catalog via
 * {@code SchoolPlatformConfigurer.additionalMetricTypes()} rather than living in platform-saas's
 * domain-neutral {@code MetricTypes} — a hospital or HR consumer of platform-saas has no notion of
 * a "student" or "teacher", so this vocabulary belongs here, not upstream. Usable with
 * {@link com.altafjava.platform.core.annotation.QuotaCheck#metric()} and
 * {@code UsageMetric.metricType} / {@code UsageAlert.metricType} once a plan's {@code limitsJson}
 * caps one of these keys.
 */
public final class SchoolMetricTypes {

	public static final String STUDENTS_COUNT = "STUDENTS_COUNT";
	public static final String TEACHERS_COUNT = "TEACHERS_COUNT";
	public static final String PARENTS_COUNT = "PARENTS_COUNT";
	public static final String CLASSES_COUNT = "CLASSES_COUNT";
	public static final String PARENT_LOGINS = "PARENT_LOGINS";

	public static final Set<String> ALL = Set.of(
			STUDENTS_COUNT, TEACHERS_COUNT, PARENTS_COUNT, CLASSES_COUNT, PARENT_LOGINS);

	private SchoolMetricTypes() {
	}
}
