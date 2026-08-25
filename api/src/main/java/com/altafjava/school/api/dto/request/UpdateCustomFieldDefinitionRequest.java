package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.customfield.model.CustomFieldType;

public record UpdateCustomFieldDefinitionRequest(
		@NotBlank @Size(max = 200) String label,
		@NotNull CustomFieldType fieldType,
		boolean required) {
}
