package com.altafjava.school.domain.curriculum.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class GradingScaleTest {

	@Test
	void create_asDefault_setsIsDefaultTrue() {
		GradingScale scale = GradingScale.create("Default", true);

		assertTrue(scale.isDefault());
		assertTrue(scale.isActive());
	}

	@Test
	void markAsDefault_thenUnmark_flipsFlag() {
		GradingScale scale = GradingScale.create("Scale", false);

		scale.markAsDefault();
		assertTrue(scale.isDefault());

		scale.unmarkAsDefault();
		assertFalse(scale.isDefault());
	}

	@Test
	void deactivate_thenActivate_flipsFlag() {
		GradingScale scale = GradingScale.create("Scale", false);

		scale.deactivate();
		assertFalse(scale.isActive());

		scale.activate();
		assertTrue(scale.isActive());
	}
}
