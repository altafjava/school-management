package com.altafjava.school.domain.holiday.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.holiday.model.Holiday;

class HolidayDateRangeResolverTest {

	private final HolidayDateRangeResolver resolver = new HolidayDateRangeResolver();

	@Test
	void resolve_nonRecurringHolidayInRange_isIncluded() {
		Holiday holiday = Holiday.create(LocalDate.of(2026, 1, 26), "Founders Day", false);

		Set<LocalDate> dates = resolver.resolve(List.of(holiday), LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 1, 31));

		assertEquals(Set.of(LocalDate.of(2026, 1, 26)), dates);
	}

	@Test
	void resolve_nonRecurringHolidayOutsideRange_isExcluded() {
		Holiday holiday = Holiday.create(LocalDate.of(2026, 2, 1), "Founders Day", false);

		Set<LocalDate> dates = resolver.resolve(List.of(holiday), LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 1, 31));

		assertTrue(dates.isEmpty());
	}

	@Test
	void resolve_recurringHoliday_matchesEveryYearInRange() {
		Holiday holiday = Holiday.create(LocalDate.of(2020, 8, 15), "Independence Day", true);

		Set<LocalDate> dates = resolver.resolve(List.of(holiday), LocalDate.of(2026, 1, 1),
				LocalDate.of(2027, 12, 31));

		assertEquals(Set.of(LocalDate.of(2026, 8, 15), LocalDate.of(2027, 8, 15)), dates);
	}
}
