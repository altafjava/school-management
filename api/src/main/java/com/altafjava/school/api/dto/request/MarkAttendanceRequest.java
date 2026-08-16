package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

public record MarkAttendanceRequest(
		@NotNull Long studentId,
		@NotNull Long classroomId,
		@NotNull LocalDate attendanceDate,
		@NotNull AttendanceStatus status,
		String markedBy) {
}
