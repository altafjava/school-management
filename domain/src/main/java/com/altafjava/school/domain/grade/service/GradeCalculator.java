package com.altafjava.school.domain.grade.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.altafjava.school.domain.grade.model.GradeThreshold;
import com.altafjava.school.domain.grade.model.GradingScale;

// Pure domain logic (no Spring, no persistence) — kept unit/mutation-testable in isolation.
public class GradeCalculator {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	public String calculateLetterGrade(BigDecimal marks, BigDecimal maxMarks, GradingScale scale) {
		if (maxMarks == null || maxMarks.signum() <= 0) {
			throw new IllegalArgumentException("maxMarks must be greater than zero");
		}
		if (marks == null || marks.signum() < 0 || marks.compareTo(maxMarks) > 0) {
			throw new IllegalArgumentException("marks must be between 0 and maxMarks");
		}
		BigDecimal percentage = marks.multiply(ONE_HUNDRED).divide(maxMarks, 4, RoundingMode.HALF_UP);
		for (GradeThreshold threshold : scale.thresholds()) {
			if (percentage.compareTo(threshold.minPercentage()) >= 0) {
				return threshold.letter();
			}
		}
		throw new IllegalStateException("Grading scale does not cover percentage: " + percentage);
	}
}
