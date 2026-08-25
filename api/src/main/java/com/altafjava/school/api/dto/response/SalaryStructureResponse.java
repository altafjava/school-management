package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryStructureResponse(
		String publicId,
		Long teacherId,
		BigDecimal basicPay,
		BigDecimal houseRentAllowance,
		BigDecimal transportAllowance,
		BigDecimal otherAllowances,
		BigDecimal otherDeductions,
		LocalDate effectiveFrom,
		boolean active) {
}
