package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record LeaveTypeResponse(
		String publicId,
		String name,
		BigDecimal defaultAnnualDays,
		boolean active) {
}
