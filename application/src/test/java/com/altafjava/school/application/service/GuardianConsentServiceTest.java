package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.GuardianConsentRecord;
import com.altafjava.school.domain.guardian.model.GuardianConsentType;
import com.altafjava.school.domain.guardian.repository.GuardianConsentRecordRepository;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class GuardianConsentServiceTest {

	private static final Long CURRENT_USER_ID = 42L;
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private GuardianConsentRecordRepository consentRecordRepository;
	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentGuardianLinkRepository studentGuardianLinkRepository;

	private GuardianConsentService guardianConsentService;

	@BeforeEach
	void setUp() {
		guardianConsentService = new GuardianConsentService(consentRecordRepository, guardianRepository,
				studentRepository, studentGuardianLinkRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		authenticateAsUser(CURRENT_USER_ID);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private Guardian guardianWithId(long id) {
		Guardian guardian = Guardian.create("Bob", "Smith", "bob@school.test", "+15551234567", CURRENT_USER_ID);
		guardian.setId(id);
		return guardian;
	}

	@Test
	void grant_guardianNotLinkedToStudent_throwsAccessDenied() {
		Student student = studentWithId(10L);
		Guardian guardian = guardianWithId(20L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(20L, 10L, 1L))
				.thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> guardianConsentService
				.grant(STUDENT_PUBLIC_ID.toString(), GuardianConsentType.DATA_PROCESSING, "2026-01"));
	}

	@Test
	void grant_noExistingRecord_createsAndGrantsNewOne() {
		Student student = studentWithId(10L);
		Guardian guardian = guardianWithId(20L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(20L, 10L, 1L)).thenReturn(true);
		when(consentRecordRepository.findByGuardianIdAndStudentIdAndConsentTypeAndTenantId(20L, 10L,
				GuardianConsentType.DATA_PROCESSING, 1L)).thenReturn(Optional.empty());
		when(consentRecordRepository.save(any(GuardianConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		GuardianConsentRecord result = guardianConsentService.grant(STUDENT_PUBLIC_ID.toString(),
				GuardianConsentType.DATA_PROCESSING, "2026-01");

		assertTrue(result.isGranted());
		assertEquals("2026-01", result.getPolicyVersion());
		assertEquals(1L, result.getTenantId());
	}

	@Test
	void grant_concurrentInsertRaceLosesToDbConstraint_retriesAsUpdateOnTheWinnerRow() {
		Student student = studentWithId(10L);
		Guardian guardian = guardianWithId(20L);
		GuardianConsentRecord raceWinner = GuardianConsentRecord.create(10L, 20L, GuardianConsentType.DATA_PROCESSING);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(20L, 10L, 1L)).thenReturn(true);
		when(consentRecordRepository.findByGuardianIdAndStudentIdAndConsentTypeAndTenantId(20L, 10L,
				GuardianConsentType.DATA_PROCESSING, 1L))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(raceWinner));
		when(consentRecordRepository.save(any(GuardianConsentRecord.class)))
				.thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"))
				.thenAnswer(inv -> inv.getArgument(0));

		GuardianConsentRecord result = guardianConsentService.grant(STUDENT_PUBLIC_ID.toString(),
				GuardianConsentType.DATA_PROCESSING, "2026-01");

		assertTrue(result.isGranted());
		assertEquals("2026-01", result.getPolicyVersion());
	}

	@Test
	void grant_existingRevokedRecord_reGrantsInPlace() {
		Student student = studentWithId(10L);
		Guardian guardian = guardianWithId(20L);
		GuardianConsentRecord existing = GuardianConsentRecord.create(10L, 20L, GuardianConsentType.DATA_PROCESSING);
		existing.grant("2025-01");
		existing.revoke();
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(20L, 10L, 1L)).thenReturn(true);
		when(consentRecordRepository.findByGuardianIdAndStudentIdAndConsentTypeAndTenantId(20L, 10L,
				GuardianConsentType.DATA_PROCESSING, 1L)).thenReturn(Optional.of(existing));
		when(consentRecordRepository.save(any(GuardianConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		GuardianConsentRecord result = guardianConsentService.grant(STUDENT_PUBLIC_ID.toString(),
				GuardianConsentType.DATA_PROCESSING, "2026-01");

		assertTrue(result.isGranted());
		assertEquals("2026-01", result.getPolicyVersion());
	}

	@Test
	void revoke_noExistingRecord_throwsResourceNotFound() {
		Student student = studentWithId(10L);
		Guardian guardian = guardianWithId(20L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(20L, 10L, 1L)).thenReturn(true);
		when(consentRecordRepository.findByGuardianIdAndStudentIdAndConsentTypeAndTenantId(20L, 10L,
				GuardianConsentType.DATA_PROCESSING, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> guardianConsentService
				.revoke(STUDENT_PUBLIC_ID.toString(), GuardianConsentType.DATA_PROCESSING));
	}
}
