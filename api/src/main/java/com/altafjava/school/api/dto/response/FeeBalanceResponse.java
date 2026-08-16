package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record FeeBalanceResponse(
		Long feeStructureId,
		String feeStructureName,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		BigDecimal outstandingBalance,
		BigDecimal overpaidAmount) {
}
