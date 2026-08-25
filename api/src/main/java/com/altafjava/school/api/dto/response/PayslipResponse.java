package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayslipResponse(
		String publicId,
		Long teacherId,
		int payYear,
		int payMonth,
		BigDecimal basicPay,
		BigDecimal houseRentAllowance,
		BigDecimal transportAllowance,
		BigDecimal otherAllowances,
		BigDecimal otherDeductions,
		BigDecimal grossPay,
		BigDecimal lossOfPayDays,
		BigDecimal lossOfPayAmount,
		BigDecimal netPay,
		String status,
		LocalDateTime finalizedAt,
		LocalDateTime disbursedAt) {
}
