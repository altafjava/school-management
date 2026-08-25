package com.altafjava.school.api.dto.response;

public record CustomFieldValueResponse(
		String fieldKey,
		String label,
		String fieldType,
		boolean required,
		String value) {
}
