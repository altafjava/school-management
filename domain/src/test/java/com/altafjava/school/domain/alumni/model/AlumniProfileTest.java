package com.altafjava.school.domain.alumni.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AlumniProfileTest {

	@Test
	void create_setsFieldsAndDefaultsActiveTrue() {
		AlumniProfile profile = AlumniProfile.create(1L, 2026, "Software Engineer", "alice@alumni.test",
				"555-0100");

		assertEquals(1L, profile.getStudentId());
		assertEquals(2026, profile.getGraduationYear());
		assertEquals("Software Engineer", profile.getCurrentOccupation());
		assertEquals("alice@alumni.test", profile.getContactEmail());
		assertTrue(profile.isActive());
	}

	@Test
	void updateContactInfo_changesOccupationAndContactDetails() {
		AlumniProfile profile = AlumniProfile.create(1L, 2026, "Software Engineer", "alice@alumni.test",
				"555-0100");

		profile.updateContactInfo("Senior Software Engineer", "alice.smith@alumni.test", "555-0200");

		assertEquals("Senior Software Engineer", profile.getCurrentOccupation());
		assertEquals("alice.smith@alumni.test", profile.getContactEmail());
		assertEquals("555-0200", profile.getContactPhone());
	}

	@Test
	void deactivate_setsActiveFalse() {
		AlumniProfile profile = AlumniProfile.create(1L, 2026, null, null, null);

		profile.deactivate();

		assertFalse(profile.isActive());
	}

	@Test
	void activate_afterDeactivate_setsActiveTrue() {
		AlumniProfile profile = AlumniProfile.create(1L, 2026, null, null, null);
		profile.deactivate();

		profile.activate();

		assertTrue(profile.isActive());
	}
}
