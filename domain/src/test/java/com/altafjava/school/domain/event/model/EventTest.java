package com.altafjava.school.domain.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EventTest {

	@Test
	void create_isActiveByDefault() {
		Event event = Event.create("Sports Day", "Annual sports event", LocalDateTime.of(2026, 12, 1, 9, 0),
				"Main Field", true, 200);

		assertTrue(event.isActive());
		assertEquals(200, event.getCapacity());
	}

	@Test
	void updateDetails_replacesMutableFields() {
		Event event = Event.create("Sports Day", "Annual sports event", LocalDateTime.of(2026, 12, 1, 9, 0),
				"Main Field", true, 200);

		event.updateDetails("Sports Day 2026", "Updated", LocalDateTime.of(2026, 12, 2, 9, 0), "New Field");

		assertEquals("Sports Day 2026", event.getTitle());
		assertEquals("New Field", event.getLocation());
	}

	@Test
	void cancel_flipsActiveFlag() {
		Event event = Event.create("Sports Day", null, LocalDateTime.of(2026, 12, 1, 9, 0), null, false, null);

		event.cancel();

		assertFalse(event.isActive());
	}
}
