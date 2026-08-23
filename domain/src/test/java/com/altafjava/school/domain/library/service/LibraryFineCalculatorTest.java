package com.altafjava.school.domain.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LibraryFineCalculatorTest {

	private final LibraryFineCalculator calculator = new LibraryFineCalculator();

	@Test
	void calculateFine_returnedOnDueDate_isZero() {
		BigDecimal fine = calculator.calculateFine(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 15),
				BigDecimal.valueOf(5));

		assertEquals(0, BigDecimal.ZERO.compareTo(fine));
	}

	@Test
	void calculateFine_returnedBeforeDueDate_isZero() {
		BigDecimal fine = calculator.calculateFine(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 10),
				BigDecimal.valueOf(5));

		assertEquals(0, BigDecimal.ZERO.compareTo(fine));
	}

	@Test
	void calculateFine_returnedAfterDueDate_multipliesOverdueDaysByRate() {
		BigDecimal fine = calculator.calculateFine(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 20),
				BigDecimal.valueOf(5));

		assertEquals(0, BigDecimal.valueOf(25).compareTo(fine));
	}
}
