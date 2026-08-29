package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayComponentAmountRequest(
		@NotBlank String code,
		@NotNull @DecimalMin(value = "0.0") BigDecimal amount) {
}
