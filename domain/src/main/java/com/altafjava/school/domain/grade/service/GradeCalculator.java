package com.altafjava.school.domain.grade.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;

// Pure domain logic (no Spring, no persistence) — kept unit/mutation-testable in isolation.
public class GradeCalculator {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	public String calculateLetterGrade(BigDecimal marks, BigDecimal maxMarks, List<GradingScaleThreshold> thresholds) {
		BigDecimal percentage = calculatePercentage(marks, maxMarks);
		return highestToLowest(thresholds).stream()
				.filter(threshold -> percentage.compareTo(threshold.getMinPercentage()) >= 0)
				.findFirst()
				.map(GradingScaleThreshold::getLetter)
				.orElseThrow(() -> new IllegalStateException("Grading scale does not cover percentage: " + percentage));
	}

	// Absent when the letter no longer exists on the (possibly since-edited) scale — GPA rollup
	// treats that grade as unresolvable and excludes it rather than failing the whole calculation.
	public Optional<BigDecimal> resolvePoints(String gradeLetter, List<GradingScaleThreshold> thresholds) {
		return thresholds.stream()
				.filter(threshold -> threshold.getLetter().equals(gradeLetter))
				.map(GradingScaleThreshold::getPoints)
				.findFirst();
	}

	private BigDecimal calculatePercentage(BigDecimal marks, BigDecimal maxMarks) {
		if (maxMarks == null || maxMarks.signum() <= 0) {
			throw new IllegalArgumentException("maxMarks must be greater than zero");
		}
		if (marks == null || marks.signum() < 0 || marks.compareTo(maxMarks) > 0) {
			throw new IllegalArgumentException("marks must be between 0 and maxMarks");
		}
		return marks.multiply(ONE_HUNDRED).divide(maxMarks, 4, RoundingMode.HALF_UP);
	}

	private List<GradingScaleThreshold> highestToLowest(List<GradingScaleThreshold> thresholds) {
		return thresholds.stream()
				.sorted(Comparator.comparing(GradingScaleThreshold::getMinPercentage).reversed())
				.toList();
	}
}
