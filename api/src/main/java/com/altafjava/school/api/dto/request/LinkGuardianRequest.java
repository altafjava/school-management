package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.guardian.model.RelationshipType;

public record LinkGuardianRequest(
		@NotBlank String studentPublicId,
		@NotNull RelationshipType relationshipType,
		boolean primaryContact) {
}
