package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record ConfigureLeaveCarryForwardRequest(
		boolean enabled,
		@DecimalMin("0.0") BigDecimal maxCarryForwardDays,
		@Min(1) Integer carryForwardExpiryMonths) {
}
