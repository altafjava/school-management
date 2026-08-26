package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

public record UpdateAttendanceStatusRequest(@NotNull AttendanceStatus status) {
}
