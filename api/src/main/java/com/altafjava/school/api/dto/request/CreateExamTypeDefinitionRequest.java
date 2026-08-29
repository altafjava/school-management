package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateExamTypeDefinitionRequest(
		@NotBlank String code,
		@NotBlank String name,
		int displayOrder) {
}
