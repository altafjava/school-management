package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;

public record FeeBalance(
		Long feeStructureId,
		String feeStructureName,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		BigDecimal outstandingBalance,
		BigDecimal overpaidAmount) {
}
