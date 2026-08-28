package com.altafjava.school.domain.fee.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeStructure;

// Computes the balance for a single FeeStructure already known to apply to the student — the
// selection of *which* structures apply (via FeeAssignment) happens in FeePaymentService.
public class FeeBalanceCalculator {

	private static final int DEFAULT_GRACE_DAYS = 0;
	private static final BigDecimal DEFAULT_LATE_FEE_PERCENTAGE = BigDecimal.ZERO;
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	/**
	 * @param assignment
	 *                       the resolved assignment this balance is computed for — nullable only for
	 *                       callers that genuinely have no assignment context (no due date is ever applied then, so
	 *                       no late fee either); prefer the non-null overload wherever an assignment is available.
	 * @param asOf
	 *                       the date late-fee applicability is evaluated against — always the actual current
	 *                       date in production, an explicit parameter here purely for deterministic testing.
	 */
	public FeeBalance calculate(FeeStructure feeStructure, FeeAssignment assignment, BigDecimal totalPaid,
			LocalDate asOf) {
		BigDecimal amountDue = feeStructure.getAmount();
		BigDecimal paid = totalPaid == null ? BigDecimal.ZERO : totalPaid;
		BigDecimal difference = amountDue.subtract(paid);
		BigDecimal baseOutstanding = difference.signum() > 0 ? difference : BigDecimal.ZERO;
		BigDecimal overpaid = difference.signum() < 0 ? difference.abs() : BigDecimal.ZERO;

		LocalDate dueDate = assignment != null ? assignment.getDueDate() : null;
		BigDecimal lateFee = BigDecimal.ZERO;
		if (dueDate != null && baseOutstanding.signum() > 0) {
			int graceDays = resolveGraceDays(assignment, feeStructure);
			if (asOf.isAfter(dueDate.plusDays(graceDays))) {
				BigDecimal lateFeePercentage = resolveLateFeePercentage(assignment, feeStructure);
				lateFee = baseOutstanding.multiply(lateFeePercentage).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
			}
		}

		return new FeeBalance(feeStructure.getId(), feeStructure.getName(), amountDue, paid,
				baseOutstanding.add(lateFee), overpaid, lateFee, dueDate);
	}

	private int resolveGraceDays(FeeAssignment assignment, FeeStructure feeStructure) {
		if (assignment.getGraceDays() != null) {
			return assignment.getGraceDays();
		}
		if (feeStructure.getGraceDays() != null) {
			return feeStructure.getGraceDays();
		}
		return DEFAULT_GRACE_DAYS;
	}

	private BigDecimal resolveLateFeePercentage(FeeAssignment assignment, FeeStructure feeStructure) {
		if (assignment.getLateFeePercentage() != null) {
			return assignment.getLateFeePercentage();
		}
		if (feeStructure.getLateFeePercentage() != null) {
			return feeStructure.getLateFeePercentage();
		}
		return DEFAULT_LATE_FEE_PERCENTAGE;
	}
}
