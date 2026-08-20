package com.altafjava.school.domain.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;

class AttendancePercentageCalculatorTest {

	private final AttendancePercentageCalculator calculator = new AttendancePercentageCalculator();

	@Test
	void calculate_allDaysPresent_returnsFullPercentage() {
		AttendancePercentage result = calculator.calculate(10, 10);

		assertEquals(0, BigDecimal.valueOf(100.00).compareTo(result.percentage()));
		assertEquals(10, result.presentDays());
		assertEquals(10, result.totalMarkedDays());
	}

	@Test
	void calculate_noDaysMarked_returnsZeroWithoutDivideByZero() {
		AttendancePercentage result = calculator.calculate(0, 0);

		assertEquals(0, BigDecimal.ZERO.compareTo(result.percentage()));
		assertEquals(0, result.totalMarkedDays());
	}

	@Test
	void calculate_partialAttendance_roundsToTwoDecimalPlaces() {
		AttendancePercentage result = calculator.calculate(1, 3);

		assertEquals(0, BigDecimal.valueOf(33.33).compareTo(result.percentage()));
	}

	@Test
	void calculate_negativePresentDays_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> calculator.calculate(-1, 10));
	}

	@Test
	void calculate_negativeTotalMarkedDays_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> calculator.calculate(0, -1));
	}

	@Test
	void calculate_presentDaysExceedsTotalMarkedDays_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> calculator.calculate(11, 10));
	}
}
