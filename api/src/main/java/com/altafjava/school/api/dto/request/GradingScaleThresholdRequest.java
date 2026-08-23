package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GradingScaleThresholdRequest(
		@NotBlank String letter,
		@NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal minPercentage,
		@NotNull @DecimalMin("0.0") BigDecimal points) {
}
