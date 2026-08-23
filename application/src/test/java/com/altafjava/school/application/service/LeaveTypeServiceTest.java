package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

	@Mock
	private LeaveTypeRepository leaveTypeRepository;

	private LeaveTypeService leaveTypeService;

	@BeforeEach
	void setUp() {
		leaveTypeService = new LeaveTypeService(leaveTypeRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNewName_succeeds() {
		when(leaveTypeRepository.existsByNameAndTenantId("Sick Leave", 1L)).thenReturn(false);
		when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(inv -> inv.getArgument(0));

		LeaveType leaveType = leaveTypeService.create("Sick Leave", BigDecimal.valueOf(12));

		assertEquals("Sick Leave", leaveType.getName());
	}

	@Test
	void create_withDuplicateName_throwsBusinessException() {
		when(leaveTypeRepository.existsByNameAndTenantId("Sick Leave", 1L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> leaveTypeService.create("Sick Leave", BigDecimal.valueOf(12)));
	}
}
