package com.altafjava.school.domain.leave.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

// Pure domain logic (no Spring, no persistence) — kept unit/mutation-testable in isolation,
// mirroring AttendancePercentageCalculator.
public class LeaveDayCalculator {

	public BigDecimal calculateDays(LocalDate startDate, LocalDate endDate, Set<LocalDate> holidayDates) {
		if (endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("endDate cannot be before startDate");
		}
		long inclusiveDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		long holidayDaysInRange = holidayDates.stream()
				.filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
				.count();
		return BigDecimal.valueOf(Math.max(inclusiveDays - holidayDaysInRange, 0));
	}
}
