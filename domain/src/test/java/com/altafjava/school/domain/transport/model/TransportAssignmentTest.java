package com.altafjava.school.domain.transport.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class TransportAssignmentTest {

	@Test
	void create_setsFields() {
		TransportAssignment assignment = TransportAssignment.create(1L, 2L, 3L, 4L, LocalDate.of(2026, 4, 1));

		assertEquals(1L, assignment.getStudentId());
		assertEquals(4L, assignment.getRouteStopId());
	}

	@Test
	void end_setsEffectiveToDate() {
		TransportAssignment assignment = TransportAssignment.create(1L, 2L, 3L, 4L, LocalDate.of(2026, 4, 1));

		assignment.end(LocalDate.of(2026, 6, 30));

		assertEquals(LocalDate.of(2026, 6, 30), assignment.getEffectiveTo());
	}

	@Test
	void end_alreadyEnded_throwsBusinessException() {
		TransportAssignment assignment = TransportAssignment.create(1L, 2L, 3L, 4L, LocalDate.of(2026, 4, 1));
		assignment.end(LocalDate.of(2026, 6, 30));

		assertThrows(BusinessException.class, () -> assignment.end(LocalDate.of(2026, 7, 1)));
	}

	@Test
	void end_beforeEffectiveFrom_throwsBusinessException() {
		TransportAssignment assignment = TransportAssignment.create(1L, 2L, 3L, 4L, LocalDate.of(2026, 4, 1));

		assertThrows(BusinessException.class, () -> assignment.end(LocalDate.of(2026, 3, 1)));
	}
}
