package com.altafjava.school.api.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateGradingScaleRequest(
		@NotBlank String name,
		boolean isDefault,
		@NotEmpty @Valid List<GradingScaleThresholdRequest> thresholds) {

	public CreateGradingScaleRequest {
		thresholds = thresholds == null ? null : List.copyOf(thresholds);
	}
}
