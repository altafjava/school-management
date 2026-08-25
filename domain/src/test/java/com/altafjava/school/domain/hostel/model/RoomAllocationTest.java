package com.altafjava.school.domain.hostel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class RoomAllocationTest {

	@Test
	void create_isActiveUntilVacated() {
		RoomAllocation allocation = RoomAllocation.create(1L, 2L, LocalDate.of(2026, 4, 1));

		assertEquals(1L, allocation.getStudentId());
		assertEquals(2L, allocation.getRoomId());
		assertTrue(allocation.isActive());
	}

	@Test
	void vacate_setsAllocatedUntilAndBecomesInactive() {
		RoomAllocation allocation = RoomAllocation.create(1L, 2L, LocalDate.of(2026, 4, 1));

		allocation.vacate(LocalDate.of(2026, 6, 30));

		assertEquals(LocalDate.of(2026, 6, 30), allocation.getAllocatedUntil());
		assertFalse(allocation.isActive());
	}

	@Test
	void vacate_alreadyVacated_throwsBusinessException() {
		RoomAllocation allocation = RoomAllocation.create(1L, 2L, LocalDate.of(2026, 4, 1));
		allocation.vacate(LocalDate.of(2026, 6, 30));

		assertThrows(BusinessException.class, () -> allocation.vacate(LocalDate.of(2026, 7, 1)));
	}

	@Test
	void vacate_beforeAllocatedFrom_throwsBusinessException() {
		RoomAllocation allocation = RoomAllocation.create(1L, 2L, LocalDate.of(2026, 4, 1));

		assertThrows(BusinessException.class, () -> allocation.vacate(LocalDate.of(2026, 3, 1)));
	}
}
