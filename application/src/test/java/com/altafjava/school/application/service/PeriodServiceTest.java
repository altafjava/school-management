package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;

@ExtendWith(MockitoExtension.class)
class PeriodServiceTest {

	@Mock
	private PeriodRepository periodRepository;

	private PeriodService periodService;

	@BeforeEach
	void setUp() {
		periodService = new PeriodService(periodRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withStartTimeBeforeEndTime_succeeds() {
		when(periodRepository.existsByNameAndTenantId("Period 1", 1L)).thenReturn(false);
		when(periodRepository.save(any(Period.class))).thenAnswer(inv -> inv.getArgument(0));

		Period period = assertDoesNotThrow(
				() -> periodService.create("Period 1", LocalTime.of(9, 0), LocalTime.of(9, 45), 1));

		assertEquals("Period 1", period.getName());
		assertEquals(1, period.getDisplayOrder());
	}

	@Test
	void create_withStartTimeNotBeforeEndTime_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> periodService.create("Period 1", LocalTime.of(10, 0), LocalTime.of(9, 0), 1));

		verify(periodRepository, never()).save(any());
	}

	@Test
	void create_withEqualStartAndEndTime_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> periodService.create("Period 1", LocalTime.of(9, 0), LocalTime.of(9, 0), 1));

		verify(periodRepository, never()).save(any());
	}

	@Test
	void create_withDuplicateName_throwsBusinessException() {
		when(periodRepository.existsByNameAndTenantId("Period 1", 1L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> periodService.create("Period 1", LocalTime.of(9, 0), LocalTime.of(9, 45), 1));

		verify(periodRepository, never()).save(any());
	}
}
