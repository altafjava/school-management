package com.altafjava.school.domain.payroll.model;

import java.math.BigDecimal;

public record PayrollComputation(
		BigDecimal grossPay,
		BigDecimal lossOfPayDays,
		BigDecimal lossOfPayAmount,
		BigDecimal netPay) {
}
