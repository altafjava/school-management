package com.altafjava.school.domain.transport.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class VehicleTest {

	@Test
	void create_isActiveByDefault() {
		Vehicle vehicle = Vehicle.create("KA-01-AB-1234", 40, "Ravi Kumar", "9999999999");

		assertTrue(vehicle.isActive());
		assertEquals(40, vehicle.getCapacity());
	}

	@Test
	void updateDetails_replacesMutableFields() {
		Vehicle vehicle = Vehicle.create("KA-01-AB-1234", 40, "Ravi Kumar", "9999999999");

		vehicle.updateDetails(45, "Suresh", "8888888888");

		assertEquals(45, vehicle.getCapacity());
		assertEquals("Suresh", vehicle.getDriverName());
	}

	@Test
	void deactivate_flipsActiveFlag() {
		Vehicle vehicle = Vehicle.create("KA-01-AB-1234", 40, "Ravi Kumar", "9999999999");

		vehicle.deactivate();

		assertFalse(vehicle.isActive());
	}
}
