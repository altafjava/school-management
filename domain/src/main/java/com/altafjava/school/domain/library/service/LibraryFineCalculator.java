package com.altafjava.school.domain.library.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Pure domain logic (no Spring, no persistence) — kept unit/mutation-testable in isolation.
public class LibraryFineCalculator {

	public BigDecimal calculateFine(LocalDate dueDate, LocalDate returnedAt, BigDecimal perDayRate) {
		if (!returnedAt.isAfter(dueDate)) {
			return BigDecimal.ZERO;
		}
		long overdueDays = ChronoUnit.DAYS.between(dueDate, returnedAt);
		return perDayRate.multiply(BigDecimal.valueOf(overdueDays));
	}
}
