package com.altafjava.school.domain.hostel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RoomTest {

	@Test
	void create_setsFieldsAndDefaultsActiveTrue() {
		Room room = Room.create(1L, "101", 4);

		assertEquals(1L, room.getHostelBuildingId());
		assertEquals("101", room.getRoomNumber());
		assertEquals(4, room.getCapacity());
		assertTrue(room.isActive());
	}

	@Test
	void updateDetails_changesRoomNumberAndCapacity() {
		Room room = Room.create(1L, "101", 4);

		room.updateDetails("102", 6);

		assertEquals("102", room.getRoomNumber());
		assertEquals(6, room.getCapacity());
	}

	@Test
	void deactivate_setsActiveFalse() {
		Room room = Room.create(1L, "101", 4);

		room.deactivate();

		assertFalse(room.isActive());
	}
}
