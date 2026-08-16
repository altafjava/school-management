package com.altafjava.school.domain.fee.service;

import java.math.BigDecimal;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeStructure;

// Every FeeStructure is assumed to apply to every student — no assignment table exists yet.
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
