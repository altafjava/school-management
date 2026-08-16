package com.altafjava.school.domain.grade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.grade.model.GradingScale;

class GradeCalculatorTest {

	private final GradeCalculator calculator = new GradeCalculator();

	@Test
	void calculateLetterGrade_atExactBoundary_awardsHigherGrade() {
		String grade = calculator.calculateLetterGrade(BigDecimal.valueOf(90), BigDecimal.valueOf(100),
				GradingScale.defaultScale());

		assertEquals("A", grade);
	}

	@Test
	void calculateLetterGrade_justBelowBoundary_awardsLowerGrade() {
		String grade = calculator.calculateLetterGrade(new BigDecimal("89.99"), BigDecimal.valueOf(100),
				GradingScale.defaultScale());

		assertEquals("B", grade);
	}

	@Test
	void calculateLetterGrade_zeroMarks_awardsLowestGrade() {
		String grade = calculator.calculateLetterGrade(BigDecimal.ZERO, BigDecimal.valueOf(100),
				GradingScale.defaultScale());

		assertEquals("F", grade);
	}

	@Test
	void calculateLetterGrade_fullMarks_awardsTopGrade() {
		String grade = calculator.calculateLetterGrade(BigDecimal.valueOf(100), BigDecimal.valueOf(100),
				GradingScale.defaultScale());

		assertEquals("A", grade);
	}

	@Test
	void calculateLetterGrade_nonStandardMaxMarks_computesPercentageCorrectly() {
		String grade = calculator.calculateLetterGrade(BigDecimal.valueOf(45), BigDecimal.valueOf(50),
				GradingScale.defaultScale());

		assertEquals("A", grade);
	}

	@Test
	void calculateLetterGrade_marksAboveMaxMarks_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> calculator.calculateLetterGrade(BigDecimal.valueOf(101), BigDecimal.valueOf(100),
						GradingScale.defaultScale()));
	}

	@Test
	void calculateLetterGrade_negativeMarks_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> calculator.calculateLetterGrade(BigDecimal.valueOf(-1), BigDecimal.valueOf(100),
						GradingScale.defaultScale()));
	}

	@Test
	void calculateLetterGrade_zeroMaxMarks_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> calculator.calculateLetterGrade(BigDecimal.valueOf(10), BigDecimal.ZERO,
						GradingScale.defaultScale()));
	}
}
