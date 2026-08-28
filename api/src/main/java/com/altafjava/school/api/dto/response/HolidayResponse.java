package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record HolidayResponse(
		String publicId,
		LocalDate date,
		String name,
		boolean recurring) {
}
