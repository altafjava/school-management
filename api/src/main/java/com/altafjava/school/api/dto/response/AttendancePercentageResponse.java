package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record AttendancePercentageResponse(
		long presentDays,
		long totalMarkedDays,
		BigDecimal percentage) {
}
