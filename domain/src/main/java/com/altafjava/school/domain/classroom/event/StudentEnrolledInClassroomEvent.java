package com.altafjava.school.domain.classroom.event;

import java.time.Instant;

// Published after a StudentClassroomLink is persisted, for future domain listeners.
public record StudentEnrolledInClassroomEvent(
		Long tenantId,
		Long studentId,
		Long classroomId,
		Long academicYearId,
		Instant timestamp) {

	public StudentEnrolledInClassroomEvent(Long tenantId, Long studentId, Long classroomId, Long academicYearId) {
		this(tenantId, studentId, classroomId, academicYearId, Instant.now());
	}
}
