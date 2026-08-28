package com.altafjava.school.domain.reportcard.service;

import java.math.BigDecimal;
import java.util.Map;

// Pure domain logic (no Spring, no persistence) — kept unit/mutation-testable in isolation,
// mirroring AttendancePercentageCalculator/LeaveDayCalculator. Standard competition ranking:
// students tied on percentage share the same rank, and the next distinct rank skips accordingly
// (e.g. two students tied for 1st means the next student is ranked 3rd, not 2nd).
public class ReportCardRankCalculator {

	public int rankOf(Long studentId, Map<Long, BigDecimal> percentageByStudentId) {
		BigDecimal ownPercentage = percentageByStudentId.get(studentId);
		if (ownPercentage == null) {
			throw new IllegalArgumentException("studentId not present in percentageByStudentId: " + studentId);
		}
		long higherCount = percentageByStudentId.values().stream()
				.filter(percentage -> percentage.compareTo(ownPercentage) > 0)
				.count();
		return (int) higherCount + 1;
	}
}
