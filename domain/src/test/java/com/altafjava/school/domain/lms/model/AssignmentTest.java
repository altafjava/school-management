package com.altafjava.school.domain.lms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AssignmentTest {

	private Assignment created() {
		return Assignment.create(1L, 2L, 3L, "Essay on Rivers", "Write 500 words", "tenants/1/assignments/key.pdf",
				LocalDate.of(2026, 3, 1), BigDecimal.valueOf(100));
	}

	@Test
	void create_setsFields() {
		Assignment assignment = created();

		assertEquals(1L, assignment.getClassroomId());
		assertEquals(2L, assignment.getSubjectId());
		assertEquals(3L, assignment.getTeacherId());
		assertEquals("Essay on Rivers", assignment.getTitle());
		assertEquals(LocalDate.of(2026, 3, 1), assignment.getDueDate());
		assertEquals(0, BigDecimal.valueOf(100).compareTo(assignment.getMaxMarks()));
	}

	@Test
	void reschedule_updatesDueDate() {
		Assignment assignment = created();

		assignment.reschedule(LocalDate.of(2026, 3, 15));

		assertEquals(LocalDate.of(2026, 3, 15), assignment.getDueDate());
	}
}
