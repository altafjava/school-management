package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePayComponentDefinitionRequest(
		@NotBlank String name,
		boolean active,
		int displayOrder) {
}
