package com.altafjava.school.api.dto.response;

import java.time.LocalTime;

public record PeriodResponse(
		String publicId,
		String name,
		LocalTime startTime,
		LocalTime endTime,
		int displayOrder) {
}
