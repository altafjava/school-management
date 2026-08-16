package com.altafjava.school.domain.subject.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SubjectTest {

	@Test
	void create_setsFieldsAndDefaultsToActive() {
		Subject subject = Subject.create("MATH-101", "Mathematics", "Core mathematics");

		assertEquals("MATH-101", subject.getCode());
		assertEquals("Mathematics", subject.getName());
		assertEquals("Core mathematics", subject.getDescription());
		assertTrue(subject.isActive());
	}

	@Test
	void deactivate_marksSubjectInactive() {
		Subject subject = Subject.create("SCI-101", "Science", null);

		subject.deactivate();

		assertFalse(subject.isActive());
	}
}
