package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeBalance(
		Long feeStructureId,
		String feeStructureName,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		BigDecimal outstandingBalance,
		BigDecimal overpaidAmount,
		BigDecimal lateFeeAmount,
		LocalDate dueDate) {
}
