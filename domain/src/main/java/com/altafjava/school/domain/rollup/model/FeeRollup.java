package com.altafjava.school.domain.rollup.model;

import java.math.BigDecimal;
import java.util.List;

// Mirrors FeeBalanceCalculator's outstanding/overpaid math (domain/fee/service/), applied at
// campus/organization aggregate level instead of per fee-structure.
public record FeeRollup(BigDecimal totalDue, BigDecimal totalPaid, BigDecimal outstandingBalance,
		BigDecimal overpaidAmount) {

	public static final FeeRollup ZERO = new FeeRollup(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
			BigDecimal.ZERO);

	public static FeeRollup of(BigDecimal totalDue, BigDecimal totalPaid) {
		BigDecimal due = totalDue == null ? BigDecimal.ZERO : totalDue;
		BigDecimal paid = totalPaid == null ? BigDecimal.ZERO : totalPaid;
		BigDecimal difference = due.subtract(paid);
		BigDecimal outstanding = difference.signum() > 0 ? difference : BigDecimal.ZERO;
		BigDecimal overpaid = difference.signum() < 0 ? difference.abs() : BigDecimal.ZERO;
		return new FeeRollup(due, paid, outstanding, overpaid);
	}

	public static FeeRollup sum(List<FeeRollup> rollups) {
		return rollups.stream().reduce(ZERO, FeeRollup::add);
	}

	private FeeRollup add(FeeRollup other) {
		return new FeeRollup(
				totalDue.add(other.totalDue),
				totalPaid.add(other.totalPaid),
				outstandingBalance.add(other.outstandingBalance),
				overpaidAmount.add(other.overpaidAmount));
	}
}
