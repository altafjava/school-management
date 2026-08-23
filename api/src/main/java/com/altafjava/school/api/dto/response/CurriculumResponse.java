package com.altafjava.school.api.dto.response;

public record CurriculumResponse(
		String publicId,
		Long boardId,
		String name,
		String code,
		String description,
		Long gradingScaleId,
		boolean active) {
}
