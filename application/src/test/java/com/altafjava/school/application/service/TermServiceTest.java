package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class TermServiceTest {

	@Mock
	private TermRepository termRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;

	private TermService termService;

	@BeforeEach
	void setUp() {
		termService = new TermService(termRepository, academicYearRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNonExistentAcademicYear_throwsResourceNotFound() {
		when(academicYearRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> termService.create("Term 1", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 30), 99L));

		verify(termRepository, never()).save(any());
	}

	@Test
	void create_duplicateNameWithinSameAcademicYear_throwsIllegalArgument() {
		when(academicYearRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(termRepository.existsByNameAndAcademicYearIdAndTenantId("Term 1", 1L, 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> termService.create("Term 1", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 30), 1L));
	}

	@Test
	void create_withValidAcademicYear_succeeds() {
		when(academicYearRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(termRepository.existsByNameAndAcademicYearIdAndTenantId("Term 1", 1L, 1L)).thenReturn(false);
		when(termRepository.save(any(Term.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> termService.create("Term 1", LocalDate.of(2025, 6, 1),
				LocalDate.of(2025, 9, 30), 1L));
	}
}
