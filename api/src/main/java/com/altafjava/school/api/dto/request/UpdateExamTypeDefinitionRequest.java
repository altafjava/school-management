package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateExamTypeDefinitionRequest(
		@NotBlank String name,
		boolean active,
		int displayOrder) {
}
