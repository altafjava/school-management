package com.altafjava.school.api.dto.request;

import java.time.LocalTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreatePeriodRequest(
		@NotBlank @Size(max = 50) String name,
		@NotNull LocalTime startTime,
		@NotNull LocalTime endTime,
		@PositiveOrZero int displayOrder) {
}
