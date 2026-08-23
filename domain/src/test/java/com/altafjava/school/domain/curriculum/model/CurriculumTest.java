package com.altafjava.school.domain.curriculum.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CurriculumTest {

	@Test
	void create_hasNoGradingScaleByDefault() {
		Curriculum curriculum = Curriculum.create(1L, "CBSE Primary", "CBSE-P", null);

		assertEquals(1L, curriculum.getBoardId());
		assertTrue(curriculum.isActive());
		assertNull(curriculum.getGradingScaleId());
	}

	@Test
	void assignGradingScale_setsId() {
		Curriculum curriculum = Curriculum.create(1L, "CBSE Primary", "CBSE-P", null);

		curriculum.assignGradingScale(42L);

		assertEquals(42L, curriculum.getGradingScaleId());
	}

	@Test
	void updateDetails_replacesMutableFields() {
		Curriculum curriculum = Curriculum.create(1L, "CBSE Primary", "CBSE-P", null);

		curriculum.updateDetails("CBSE Primary Renamed", "CBSE-P2", "Updated");

		assertEquals("CBSE Primary Renamed", curriculum.getName());
		assertEquals("CBSE-P2", curriculum.getCode());
		assertEquals("Updated", curriculum.getDescription());
	}

	@Test
	void deactivate_flipsActiveFlag() {
		Curriculum curriculum = Curriculum.create(1L, "CBSE Primary", "CBSE-P", null);

		curriculum.deactivate();

		assertFalse(curriculum.isActive());
	}
}
