package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.hostel.model.HostelBuilding;
import com.altafjava.school.domain.hostel.model.Room;
import com.altafjava.school.domain.hostel.repository.HostelBuildingRepository;
import com.altafjava.school.domain.hostel.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

	private static final UUID BUILDING_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private RoomRepository roomRepository;
	@Mock
	private HostelBuildingRepository hostelBuildingRepository;

	private RoomService roomService;

	@BeforeEach
	void setUp() {
		roomService = new RoomService(roomRepository, hostelBuildingRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private HostelBuilding buildingWithId(long id) {
		HostelBuilding building = HostelBuilding.create("North Block", "12 Campus Road");
		building.setId(id);
		return building;
	}

	@Test
	void create_withValidBuilding_succeeds() {
		when(hostelBuildingRepository.findByPublicIdAndTenantId(eq(BUILDING_PUBLIC_ID), eq(1L)))
				.thenReturn(Optional.of(buildingWithId(10L)));
		when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

		Room room = roomService.create(BUILDING_PUBLIC_ID.toString(), "101", 4);

		assertEquals(10L, room.getHostelBuildingId());
		assertEquals("101", room.getRoomNumber());
	}

	@Test
	void create_unknownBuilding_throwsResourceNotFoundException() {
		when(hostelBuildingRepository.findByPublicIdAndTenantId(eq(BUILDING_PUBLIC_ID), eq(1L)))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> roomService.create(BUILDING_PUBLIC_ID.toString(), "101", 4));
	}
}
