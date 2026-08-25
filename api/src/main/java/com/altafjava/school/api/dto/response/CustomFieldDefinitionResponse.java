package com.altafjava.school.api.dto.response;

public record CustomFieldDefinitionResponse(
		String publicId,
		String entityType,
		String fieldKey,
		String label,
		String fieldType,
		boolean required,
		boolean active) {
}
