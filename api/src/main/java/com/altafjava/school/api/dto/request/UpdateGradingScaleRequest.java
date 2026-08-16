package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateGradingScaleRequest(
		@NotEmpty @Valid List<Threshold> thresholds) {

	public UpdateGradingScaleRequest {
		// Defensive copy, not a null-check — @NotEmpty must still see and reject a null list.
		thresholds = thresholds == null ? null : List.copyOf(thresholds);
	}

	public record Threshold(
			@NotBlank String letter,
			@NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal minPercentage) {
	}
}
