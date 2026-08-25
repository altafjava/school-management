package com.altafjava.school.domain.hostel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class HostelBuildingTest {

	@Test
	void create_setsFieldsAndDefaultsActiveTrue() {
		HostelBuilding building = HostelBuilding.create("North Block", "12 Campus Road");

		assertEquals("North Block", building.getName());
		assertEquals("12 Campus Road", building.getAddress());
		assertTrue(building.isActive());
	}

	@Test
	void updateDetails_changesNameAndAddress() {
		HostelBuilding building = HostelBuilding.create("North Block", "12 Campus Road");

		building.updateDetails("North Block Renamed", "14 Campus Road");

		assertEquals("North Block Renamed", building.getName());
		assertEquals("14 Campus Road", building.getAddress());
	}

	@Test
	void deactivate_setsActiveFalse() {
		HostelBuilding building = HostelBuilding.create("North Block", "12 Campus Road");

		building.deactivate();

		assertFalse(building.isActive());
	}
}
