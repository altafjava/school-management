package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record TermResponse(
		String publicId,
		String name,
		LocalDate startDate,
		LocalDate endDate,
		Long academicYearId,
		boolean current) {
}
