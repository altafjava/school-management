package com.altafjava.school.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.customfield.model.CustomFieldType;

public record CreateCustomFieldDefinitionRequest(
		@NotNull CustomFieldEntityType entityType,
		@NotBlank @Size(max = 100) String fieldKey,
		@NotBlank @Size(max = 200) String label,
		@NotNull CustomFieldType fieldType,
		boolean required,
		@Valid CustomFieldValidationRuleRequest validationRule,
		int displayOrder,
		@Size(max = 100) String displayGroup,
		int displayGroupOrder,
		@Valid CustomFieldVisibilityConditionRequest visibilityCondition) {
}
