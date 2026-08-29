package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.payroll.model.PayComponentType;

public record CreatePayComponentDefinitionRequest(
		@NotBlank String code,
		@NotBlank String name,
		@NotNull PayComponentType type,
		int displayOrder) {
}
