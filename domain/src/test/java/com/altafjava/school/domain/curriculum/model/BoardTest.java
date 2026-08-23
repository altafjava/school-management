package com.altafjava.school.domain.curriculum.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class BoardTest {

	@Test
	void create_isActiveByDefault() {
		Board board = Board.create("CBSE", "CBSE", "Central Board of Secondary Education");

		assertTrue(board.isActive());
		assertEquals("CBSE", board.getName());
	}

	@Test
	void updateDetails_replacesMutableFields() {
		Board board = Board.create("CBSE", "CBSE", null);

		board.updateDetails("CBSE Renamed", "CBSE2", "Updated description");

		assertEquals("CBSE Renamed", board.getName());
		assertEquals("CBSE2", board.getCode());
		assertEquals("Updated description", board.getDescription());
	}

	@Test
	void deactivate_thenActivate_flipsFlag() {
		Board board = Board.create("CBSE", "CBSE", null);

		board.deactivate();
		assertFalse(board.isActive());

		board.activate();
		assertTrue(board.isActive());
	}
}
