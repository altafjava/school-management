package com.altafjava.school.domain.customfield.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CustomFieldDefinitionTest {

	@Test
	void create_setsFieldsAndDefaultsActiveTrue() {
		CustomFieldDefinition definition = CustomFieldDefinition.create(CustomFieldEntityType.STUDENT, "bloodGroup",
				"Blood Group", CustomFieldType.TEXT, true);

		assertEquals(CustomFieldEntityType.STUDENT, definition.getEntityType());
		assertEquals("bloodGroup", definition.getFieldKey());
		assertEquals("Blood Group", definition.getLabel());
		assertEquals(CustomFieldType.TEXT, definition.getFieldType());
		assertTrue(definition.isRequired());
		assertTrue(definition.isActive());
	}

	@Test
	void updateDetails_replacesLabelFieldTypeAndRequired() {
		CustomFieldDefinition definition = CustomFieldDefinition.create(CustomFieldEntityType.STUDENT, "allergy",
				"Allergy", CustomFieldType.TEXT, false);

		definition.updateDetails("Known Allergies", CustomFieldType.TEXT, true);

		assertEquals("Known Allergies", definition.getLabel());
		assertTrue(definition.isRequired());
	}

	@Test
	void deactivateThenActivate_flipsActiveFlag() {
		CustomFieldDefinition definition = CustomFieldDefinition.create(CustomFieldEntityType.TEACHER, "license",
				"License No.", CustomFieldType.TEXT, false);

		definition.deactivate();
		assertFalse(definition.isActive());

		definition.activate();
		assertTrue(definition.isActive());
	}
}
