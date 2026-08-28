package com.altafjava.school.domain.customfield.model;

/**
 * Closed set of platform/school entity types a tenant admin may attach custom fields to. Kept
 * closed (rather than an open string) so a typo in {@code entityType} at write time is a compile
 * error, not a silently-orphaned definition nobody's data ever matches — mirrors every other
 * enum-backed catalog in this codebase ({@code EnrollmentStatus}, {@code CustomFieldType} below).
 * Extend this enum, not the design, when a further entity type is needed.
 */
public enum CustomFieldEntityType {
	STUDENT, TEACHER, GUARDIAN, ADMISSION, CLASSROOM, SUBJECT, FEE_STRUCTURE, LEAVE_TYPE, HOLIDAY, TRANSPORT_ROUTE, TRANSPORT_VEHICLE, BOOK
}
