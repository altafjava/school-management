package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSalaryStructureRequest(
		@NotBlank String teacherPublicId,
		@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal basicPay,
		@NotNull @DecimalMin(value = "0.0") BigDecimal houseRentAllowance,
		@NotNull @DecimalMin(value = "0.0") BigDecimal transportAllowance,
		@NotNull @DecimalMin(value = "0.0") BigDecimal otherAllowances,
		@NotNull @DecimalMin(value = "0.0") BigDecimal otherDeductions,
		@NotNull LocalDate effectiveFrom) {
}
