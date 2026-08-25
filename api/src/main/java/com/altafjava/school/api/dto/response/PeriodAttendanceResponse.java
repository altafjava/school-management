package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record PeriodAttendanceResponse(
		String publicId,
		Long studentId,
		Long classroomId,
		Long timetableEntryId,
		LocalDate attendanceDate,
		String status,
		String markedBy) {
}
