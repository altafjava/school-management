package com.altafjava.school.api.dto.response;

import java.util.List;

public record CustomFieldValueResponse(
		String fieldKey,
		String label,
		String fieldType,
		boolean required,
		String value,
		List<String> options,
		int displayOrder,
		String displayGroup) {

	public CustomFieldValueResponse {
		options = List.copyOf(options);
	}
}
