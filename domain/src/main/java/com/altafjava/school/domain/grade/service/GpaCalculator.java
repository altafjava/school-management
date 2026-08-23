package com.altafjava.school.domain.grade.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

// Pure domain logic (no Spring, no persistence) — unweighted average of resolved grade points.
public class GpaCalculator {

	public Optional<BigDecimal> calculateAverage(List<BigDecimal> points) {
		if (points.isEmpty()) {
			return Optional.empty();
		}
		BigDecimal sum = points.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return Optional.of(sum.divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP));
	}
}
