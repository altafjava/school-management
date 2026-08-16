package com.altafjava.school.domain.grade.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GradingScaleTest {

	@Test
	void of_emptyThresholds_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> GradingScale.of(List.of()));
	}

	@Test
	void of_thresholdsNotCoveringZero_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> GradingScale.of(List.of(new GradeThreshold("A", BigDecimal.valueOf(50)))));
	}

	@Test
	void of_unsortedThresholds_sortsDescendingByMinPercentage() {
		GradingScale scale = GradingScale.of(List.of(
				new GradeThreshold("F", BigDecimal.ZERO),
				new GradeThreshold("A", BigDecimal.valueOf(90)),
				new GradeThreshold("B", BigDecimal.valueOf(80))));

		assertEquals("A", scale.thresholds().get(0).letter());
		assertEquals("B", scale.thresholds().get(1).letter());
		assertEquals("F", scale.thresholds().get(2).letter());
	}

	@Test
	void defaultScale_coversZeroToHundred() {
		GradingScale scale = GradingScale.defaultScale();

		assertEquals(BigDecimal.ZERO, scale.thresholds().get(scale.thresholds().size() - 1).minPercentage());
	}
}
