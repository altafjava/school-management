package com.altafjava.school.domain.inventory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class AssetAssignmentTest {

	@Test
	void create_setsFields() {
		AssetAssignment assignment = AssetAssignment.create(1L, AssignedToType.STAFF, 2L, LocalDate.of(2026, 4, 1));

		assertEquals(AssignedToType.STAFF, assignment.getAssignedToType());
		assertEquals(2L, assignment.getAssignedToId());
	}

	@Test
	void markReturned_setsReturnedAt() {
		AssetAssignment assignment = AssetAssignment.create(1L, AssignedToType.CLASSROOM, 2L, LocalDate.of(2026, 4, 1));

		assignment.markReturned(LocalDate.of(2026, 5, 1));

		assertEquals(LocalDate.of(2026, 5, 1), assignment.getReturnedAt());
	}

	@Test
	void markReturned_alreadyReturned_throwsBusinessException() {
		AssetAssignment assignment = AssetAssignment.create(1L, AssignedToType.STAFF, 2L, LocalDate.of(2026, 4, 1));
		assignment.markReturned(LocalDate.of(2026, 5, 1));

		assertThrows(BusinessException.class, () -> assignment.markReturned(LocalDate.of(2026, 5, 2)));
	}
}
