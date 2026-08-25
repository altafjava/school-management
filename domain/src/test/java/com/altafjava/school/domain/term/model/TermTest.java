package com.altafjava.school.domain.term.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TermTest {

	private Term newTerm() {
		return Term.create("Term 1", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 30), 1L);
	}

	@Test
	void create_defaultsCurrentToFalse() {
		Term term = newTerm();

		assertFalse(term.isCurrent());
	}

	@Test
	void markCurrent_setsCurrentTrue() {
		Term term = newTerm();

		term.markCurrent();

		assertTrue(term.isCurrent());
	}

	@Test
	void markNotCurrent_setsCurrentFalse() {
		Term term = newTerm();
		term.markCurrent();

		term.markNotCurrent();

		assertFalse(term.isCurrent());
	}
}
