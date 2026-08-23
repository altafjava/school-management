package com.altafjava.school.api.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record UpdateGradingScaleThresholdsRequest(
		@NotEmpty @Valid List<GradingScaleThresholdRequest> thresholds) {

	public UpdateGradingScaleThresholdsRequest {
		thresholds = thresholds == null ? null : List.copyOf(thresholds);
	}
}
