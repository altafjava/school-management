package com.altafjava.school.api.dto.response;

public record ExamTypeDefinitionResponse(
		String publicId,
		String code,
		String name,
		int displayOrder,
		boolean active) {
}
