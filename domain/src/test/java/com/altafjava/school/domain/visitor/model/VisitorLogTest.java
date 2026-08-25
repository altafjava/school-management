package com.altafjava.school.domain.visitor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class VisitorLogTest {

	@Test
	void checkIn_setsFieldsAndLeavesCheckOutNull() {
		LocalDateTime checkInAt = LocalDateTime.of(2026, 5, 1, 9, 0);

		VisitorLog log = VisitorLog.checkIn("Alex Ray", "555-0100", "Parent-teacher meeting", 7L, checkInAt);

		assertEquals("Alex Ray", log.getVisitorName());
		assertEquals("555-0100", log.getVisitorPhone());
		assertEquals("Parent-teacher meeting", log.getPurpose());
		assertEquals(7L, log.getHostTeacherId());
		assertEquals(checkInAt, log.getCheckInAt());
		assertNull(log.getCheckOutAt());
	}

	@Test
	void checkOut_setsCheckOutAt() {
		LocalDateTime checkInAt = LocalDateTime.of(2026, 5, 1, 9, 0);
		VisitorLog log = VisitorLog.checkIn("Alex Ray", "555-0100", "Parent-teacher meeting", 7L, checkInAt);

		LocalDateTime checkOutAt = LocalDateTime.of(2026, 5, 1, 9, 45);
		log.checkOut(checkOutAt);

		assertEquals(checkOutAt, log.getCheckOutAt());
	}

	@Test
	void checkOut_alreadyCheckedOut_throwsBusinessException() {
		LocalDateTime checkInAt = LocalDateTime.of(2026, 5, 1, 9, 0);
		VisitorLog log = VisitorLog.checkIn("Alex Ray", "555-0100", "Parent-teacher meeting", 7L, checkInAt);
		log.checkOut(LocalDateTime.of(2026, 5, 1, 9, 45));

		assertThrows(BusinessException.class, () -> log.checkOut(LocalDateTime.of(2026, 5, 1, 10, 0)));
	}

	@Test
	void checkOut_beforeCheckIn_throwsBusinessException() {
		LocalDateTime checkInAt = LocalDateTime.of(2026, 5, 1, 9, 0);
		VisitorLog log = VisitorLog.checkIn("Alex Ray", "555-0100", "Parent-teacher meeting", 7L, checkInAt);

		assertThrows(BusinessException.class, () -> log.checkOut(LocalDateTime.of(2026, 5, 1, 8, 0)));
	}
}
