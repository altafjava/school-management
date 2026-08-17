package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.admission.model.DecisionOutcome;

public record DecideAdmissionRequest(
		@NotNull DecisionOutcome outcome,
		@NotBlank @Size(max = 100) String decidedBy,
		@Size(max = 1000) String notes,
		@Size(max = 50) String studentCode) {
}
