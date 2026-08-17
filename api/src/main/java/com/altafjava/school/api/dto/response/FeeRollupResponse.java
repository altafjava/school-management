package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record FeeRollupResponse(BigDecimal totalDue, BigDecimal totalPaid, BigDecimal outstandingBalance,
		BigDecimal overpaidAmount) {
}
