package com.altafjava.school.domain.payroll.model;

import java.math.BigDecimal;

/**
 * The allowance/deduction line items copied out of a {@link SalaryStructure} at payslip generation
 * time — a distinct type (rather than passing the structure itself into {@code Payslip}) so a later
 * revision to the teacher's structure can never retroactively change an already-generated payslip.
 */
public record SalarySnapshot(
		BigDecimal basicPay,
		BigDecimal houseRentAllowance,
		BigDecimal transportAllowance,
		BigDecimal otherAllowances,
		BigDecimal otherDeductions) {
}
