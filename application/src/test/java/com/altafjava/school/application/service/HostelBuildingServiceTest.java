package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.altafjava.school.domain.hostel.repository.HostelBuildingRepository;

@ExtendWith(MockitoExtension.class)
class HostelBuildingServiceTest {

	@Mock
	private HostelBuildingRepository hostelBuildingRepository;

	private HostelBuildingService hostelBuildingService;

	@BeforeEach
	void setUp() {
		hostelBuildingService = new HostelBuildingService(hostelBuildingRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_savesNewBuilding() {
		when(hostelBuildingRepository.save(any(HostelBuilding.class))).thenAnswer(inv -> inv.getArgument(0));

		HostelBuilding building = hostelBuildingService.create("North Block", "12 Campus Road");

		assertEquals("North Block", building.getName());
	}

	@Test
	void findByPublicId_unknownPublicId_throwsResourceNotFoundException() {
		UUID publicId = UUID.randomUUID();
		when(hostelBuildingRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> hostelBuildingService.findByPublicId(publicId.toString()));
	}

	@Test
	void deactivate_setsActiveFalse() {
		UUID publicId = UUID.randomUUID();
		HostelBuilding building = HostelBuilding.create("North Block", "12 Campus Road");
		when(hostelBuildingRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(building));
		when(hostelBuildingRepository.save(any(HostelBuilding.class))).thenAnswer(inv -> inv.getArgument(0));

		HostelBuilding deactivated = hostelBuildingService.deactivate(publicId.toString());

		assertEquals(false, deactivated.isActive());
	}
}
