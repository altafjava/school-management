package com.altafjava.school.domain.grade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GpaCalculatorTest {

	private final GpaCalculator calculator = new GpaCalculator();

	@Test
	void calculateAverage_emptyList_returnsEmpty() {
		assertTrue(calculator.calculateAverage(List.of()).isEmpty());
	}

	@Test
	void calculateAverage_singlePoint_returnsThatPoint() {
		var average = calculator.calculateAverage(List.of(new BigDecimal("4.0")));

		assertEquals(0, new BigDecimal("4.00").compareTo(average.get()));
	}

	@Test
	void calculateAverage_multiplePoints_returnsRoundedMean() {
		var average = calculator.calculateAverage(
				List.of(new BigDecimal("4.0"), new BigDecimal("3.0"), new BigDecimal("3.0")));

		assertEquals(0, new BigDecimal("3.33").compareTo(average.get()));
	}
}
