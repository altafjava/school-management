package com.altafjava.school.api.dto.response;

public record HealthRecordResponse(
		String publicId,
		Long studentId,
		String bloodGroup,
		String allergies,
		String conditions,
		String immunizations) {
}
