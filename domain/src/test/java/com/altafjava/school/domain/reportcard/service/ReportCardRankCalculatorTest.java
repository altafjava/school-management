package com.altafjava.school.domain.reportcard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportCardRankCalculatorTest {

	private final ReportCardRankCalculator calculator = new ReportCardRankCalculator();

	@Test
	void rankOf_highestPercentage_isRankOne() {
		Map<Long, BigDecimal> percentages = Map.of(1L, BigDecimal.valueOf(90), 2L, BigDecimal.valueOf(80),
				3L, BigDecimal.valueOf(70));

		assertEquals(1, calculator.rankOf(1L, percentages));
		assertEquals(2, calculator.rankOf(2L, percentages));
		assertEquals(3, calculator.rankOf(3L, percentages));
	}

	@Test
	void rankOf_tiedPercentages_shareRankAndSkipNext() {
		Map<Long, BigDecimal> percentages = Map.of(1L, BigDecimal.valueOf(90), 2L, BigDecimal.valueOf(90),
				3L, BigDecimal.valueOf(70));

		assertEquals(1, calculator.rankOf(1L, percentages));
		assertEquals(1, calculator.rankOf(2L, percentages));
		assertEquals(3, calculator.rankOf(3L, percentages));
	}

	@Test
	void rankOf_studentNotInMap_throwsIllegalArgument() {
		Map<Long, BigDecimal> percentages = Map.of(1L, BigDecimal.valueOf(90));

		assertThrows(IllegalArgumentException.class, () -> calculator.rankOf(99L, percentages));
	}
}
