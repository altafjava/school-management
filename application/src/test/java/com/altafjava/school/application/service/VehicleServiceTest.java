package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.transport.model.Vehicle;
import com.altafjava.school.domain.transport.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

	@Mock
	private VehicleRepository vehicleRepository;

	private VehicleService vehicleService;

	@BeforeEach
	void setUp() {
		vehicleService = new VehicleService(vehicleRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNewRegistrationNumber_succeeds() {
		when(vehicleRepository.existsByRegistrationNumberAndTenantId("KA-01-AB-1234", 1L)).thenReturn(false);
		when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

		Vehicle vehicle = vehicleService.create("KA-01-AB-1234", 40, "Ravi", "9999999999");

		assertEquals("KA-01-AB-1234", vehicle.getRegistrationNumber());
	}

	@Test
	void create_withDuplicateRegistrationNumber_throwsBusinessException() {
		when(vehicleRepository.existsByRegistrationNumberAndTenantId("KA-01-AB-1234", 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> vehicleService.create("KA-01-AB-1234", 40, "Ravi",
				"9999999999"));
	}
}
