package com.altafjava.school.domain.leave.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LeaveDayCalculatorTest {

	private final LeaveDayCalculator calculator = new LeaveDayCalculator();

	@Test
	void calculateDays_withNoHolidays_countsInclusiveDays() {
		BigDecimal days = calculator.calculateDays(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), Set.of());

		assertEquals(0, BigDecimal.valueOf(3).compareTo(days));
	}

	@Test
	void calculateDays_withHolidayInRange_excludesIt() {
		BigDecimal days = calculator.calculateDays(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3),
				Set.of(LocalDate.of(2026, 6, 2)));

		assertEquals(0, BigDecimal.valueOf(2).compareTo(days));
	}

	@Test
	void calculateDays_withHolidayOutsideRange_isIgnored() {
		BigDecimal days = calculator.calculateDays(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3),
				Set.of(LocalDate.of(2026, 6, 10)));

		assertEquals(0, BigDecimal.valueOf(3).compareTo(days));
	}

	@Test
	void calculateDays_withEndDateBeforeStartDate_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> calculator.calculateDays(LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 1), Set.of()));
	}

	@Test
	void calculateDays_everyDayIsHoliday_neverGoesNegative() {
		BigDecimal days = calculator.calculateDays(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2),
				Set.of(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2)));

		assertEquals(0, BigDecimal.ZERO.compareTo(days));
	}
}
