package com.altafjava.school.domain.attendance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;

// Pure domain logic (no Spring, no persistence) — kept unit/mutation-testable in isolation.
public class AttendancePercentageCalculator {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	public AttendancePercentage calculate(long presentDays, long totalMarkedDays) {
		if (presentDays < 0 || totalMarkedDays < 0) {
			throw new IllegalArgumentException("presentDays and totalMarkedDays must not be negative");
		}
		if (presentDays > totalMarkedDays) {
			throw new IllegalArgumentException("presentDays cannot exceed totalMarkedDays");
		}
		if (totalMarkedDays == 0) {
			return new AttendancePercentage(0, 0, BigDecimal.ZERO);
		}
		BigDecimal percentage = BigDecimal.valueOf(presentDays)
				.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(totalMarkedDays), 2, RoundingMode.HALF_UP);
		return new AttendancePercentage(presentDays, totalMarkedDays, percentage);
	}
}
