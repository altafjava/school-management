package com.altafjava.school.domain.timetable.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class PeriodTest {

	@Test
	void create_setsAllFields() {
		Period period = Period.create("Period 1", LocalTime.of(9, 0), LocalTime.of(9, 45), 1);

		assertEquals("Period 1", period.getName());
		assertEquals(LocalTime.of(9, 0), period.getStartTime());
		assertEquals(LocalTime.of(9, 45), period.getEndTime());
		assertEquals(1, period.getDisplayOrder());
	}
}
