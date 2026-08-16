package com.altafjava.school.domain.grade.model;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

// Thresholds are kept sorted highest-percentage-first so GradeCalculator returns the first match.
public final class GradingScale {

	private final List<GradeThreshold> thresholds;

	private GradingScale(List<GradeThreshold> thresholds) {
		this.thresholds = thresholds;
	}

	public static GradingScale of(List<GradeThreshold> thresholds) {
		if (thresholds == null || thresholds.isEmpty()) {
			throw new IllegalArgumentException("Grading scale must contain at least one threshold");
		}
		List<GradeThreshold> sorted = thresholds.stream()
				.sorted(Comparator.comparing(GradeThreshold::minPercentage).reversed())
				.toList();
		if (sorted.get(sorted.size() - 1).minPercentage().compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException("Grading scale must cover down to 0%");
		}
		return new GradingScale(sorted);
	}

	public static GradingScale defaultScale() {
		return of(List.of(
				new GradeThreshold("A", new BigDecimal("90")),
				new GradeThreshold("B", new BigDecimal("80")),
				new GradeThreshold("C", new BigDecimal("70")),
				new GradeThreshold("D", new BigDecimal("60")),
				new GradeThreshold("F", BigDecimal.ZERO)));
	}

	public List<GradeThreshold> thresholds() {
		return List.copyOf(thresholds);
	}
}
