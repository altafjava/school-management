package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

	@Mock
	private AcademicYearRepository academicYearRepository;

	private AcademicYearService academicYearService;

	@BeforeEach
	void setUp() {
		academicYearService = new AcademicYearService(academicYearRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_asCurrent_unsetsPreviouslyCurrentYear() {
		AcademicYear previousCurrent = AcademicYear.create("2024-25",
				LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), true);
		when(academicYearRepository.existsByNameAndTenantId("2025-26", 1L)).thenReturn(false);
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.of(previousCurrent));
		when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

		academicYearService.create("2025-26", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true);

		assertFalse(previousCurrent.isCurrent(),
				"Creating a new current academic year must unset the previous one — "
						+ "otherwise two years end up marked current simultaneously");
		verify(academicYearRepository).save(previousCurrent);
	}

	@Test
	void create_asCurrent_withNoExistingCurrentYear_doesNotFailOrSaveAnExtraRecord() {
		when(academicYearRepository.existsByNameAndTenantId("2025-26", 1L)).thenReturn(false);
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.empty());
		when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

		AcademicYear result = academicYearService.create("2025-26",
				LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true);

		assertTrue(result.isCurrent());
		verify(academicYearRepository, never()).findByPublicIdAndTenantId(any(), any());
	}

	@Test
	void create_notCurrent_neverQueriesForExistingCurrentYear() {
		when(academicYearRepository.existsByNameAndTenantId("2025-26", 1L)).thenReturn(false);
		when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

		academicYearService.create("2025-26", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), false);

		verify(academicYearRepository, never()).findByCurrentTrueAndTenantId(any());
	}

	@Test
	void create_duplicateName_throws() {
		when(academicYearRepository.existsByNameAndTenantId("2025-26", 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> academicYearService.create("2025-26", LocalDate.of(2025, 6, 1),
						LocalDate.of(2026, 5, 31), false));
	}
}
