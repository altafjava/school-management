package com.altafjava.school.domain.fee.service;

import java.math.BigDecimal;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeStructure;

// Computes the balance for a single FeeStructure already known to apply to the student — the
// selection of *which* structures apply (via FeeAssignment) happens in FeePaymentService.
public class FeeBalanceCalculator {

	public FeeBalance calculate(FeeStructure feeStructure, BigDecimal totalPaid) {
		BigDecimal amountDue = feeStructure.getAmount();
		BigDecimal paid = totalPaid == null ? BigDecimal.ZERO : totalPaid;
		BigDecimal difference = amountDue.subtract(paid);
		BigDecimal outstanding = difference.signum() > 0 ? difference : BigDecimal.ZERO;
		BigDecimal overpaid = difference.signum() < 0 ? difference.abs() : BigDecimal.ZERO;
		return new FeeBalance(feeStructure.getId(), feeStructure.getName(), amountDue, paid, outstanding, overpaid);
	}
}
