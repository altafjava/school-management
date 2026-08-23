package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.inventory.model.AssignedToType;

public record AssignAssetRequest(
		@NotNull AssignedToType assignedToType,
		@NotNull Long assignedToId,
		@NotNull LocalDate assignedAt) {
}
