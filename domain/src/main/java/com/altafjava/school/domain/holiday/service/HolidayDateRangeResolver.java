package com.altafjava.school.domain.holiday.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.altafjava.school.domain.holiday.model.Holiday;

// Pure domain logic (no Spring, no persistence) — the actual date-matching behind
// HolidayService#datesInRange, factored out so other services needing the same computation (e.g.
// ReportCardService's attendance-summary section) can reuse it via the Holiday domain repository
// directly instead of depending on the HolidayService application service.
public class HolidayDateRangeResolver {

	public Set<LocalDate> resolve(List<Holiday> holidays, LocalDate from, LocalDate to) {
		Set<LocalDate> dates = new HashSet<>();
		for (Holiday holiday : holidays) {
			if (holiday.isRecurring()) {
				for (LocalDate candidate = from; !candidate.isAfter(to); candidate = candidate.plusDays(1)) {
					if (holiday.fallsOn(candidate)) {
						dates.add(candidate);
					}
				}
			} else if (!holiday.getDate().isBefore(from) && !holiday.getDate().isAfter(to)) {
				dates.add(holiday.getDate());
			}
		}
		return dates;
	}
}
