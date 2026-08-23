package com.altafjava.school.domain.curriculum.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GradingScaleThresholdTest {

	@Test
	void create_setsFields() {
		GradingScaleThreshold threshold = GradingScaleThreshold.create(5L, "A", new BigDecimal("90"),
				new BigDecimal("4.0"));

		assertEquals(5L, threshold.getGradingScaleId());
		assertEquals("A", threshold.getLetter());
		assertEquals(0, new BigDecimal("90").compareTo(threshold.getMinPercentage()));
		assertEquals(0, new BigDecimal("4.0").compareTo(threshold.getPoints()));
	}
}
