package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateLowAttendanceThresholdRequest(
		@NotNull @Min(0) @Max(100) Integer thresholdPercent) {
}
