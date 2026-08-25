package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.service.UserDomainService;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.GuardianSelfRegistrationMode;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;

@ExtendWith(MockitoExtension.class)
class GuardianSelfRegistrationServiceTest {

	private static final String EMAIL = "jane@school.test";

	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private StudentGuardianLinkRepository studentGuardianLinkRepository;
	@Mock
	private GuardianRegistrationSettingsService guardianRegistrationSettingsService;
	@Mock
	private UserDomainService userService;

	private GuardianSelfRegistrationService guardianSelfRegistrationService;

	@BeforeEach
	void setUp() {
		guardianSelfRegistrationService = new GuardianSelfRegistrationService(guardianRepository,
				studentGuardianLinkRepository, guardianRegistrationSettingsService, userService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void register_withPendingGuardianRecord_claimsItAndGrantsParentRole() {
		Guardian pending = Guardian.create("Jane", "Doe", EMAIL, "555-0100", null);
		pending.setId(10L);
		when(guardianRepository.findByEmailAndTenantIdAndUserIdIsNull(EMAIL, 1L)).thenReturn(Optional.of(pending));
		User createdUser = userOf(100L);
		when(userService.createUser(eq(1L), eq(EMAIL), eq("Password123!"), eq("Jane"), eq("Doe"),
				eq(Set.of(SchoolRoles.PARENT)))).thenReturn(createdUser);
		when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));

		Guardian result = guardianSelfRegistrationService.register(EMAIL, "Password123!", "Jane", "Doe", "555-0100");

		assertEquals(100L, result.getUserId());
		verify(guardianRegistrationSettingsService, never()).getMode(any());
	}

	@Test
	void register_withPendingGuardianRecord_stampsConsentOnExistingLinks() {
		Guardian pending = Guardian.create("Jane", "Doe", EMAIL, "555-0100", null);
		pending.setId(10L);
		when(guardianRepository.findByEmailAndTenantIdAndUserIdIsNull(EMAIL, 1L)).thenReturn(Optional.of(pending));
		User createdUser = userOf(100L);
		when(userService.createUser(eq(1L), eq(EMAIL), eq("Password123!"), eq("Jane"), eq("Doe"),
				eq(Set.of(SchoolRoles.PARENT)))).thenReturn(createdUser);
		when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));
		StudentGuardianLink link = StudentGuardianLink.create(20L, 10L, RelationshipType.MOTHER, true);
		when(studentGuardianLinkRepository.findAllByGuardianIdAndTenantId(10L, 1L))
				.thenReturn(List.of(link));
		ArgumentCaptor<List<StudentGuardianLink>> savedLinksCaptor = ArgumentCaptor.forClass(List.class);
		when(studentGuardianLinkRepository.saveAll(savedLinksCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

		guardianSelfRegistrationService.register(EMAIL, "Password123!", "Jane", "Doe", "555-0100");

		assertNotNull(savedLinksCaptor.getValue().get(0).getConsentGivenAt());
	}

	@Test
	void register_noPendingRecordAndClaimOnlyMode_throwsBusinessException() {
		when(guardianRepository.findByEmailAndTenantIdAndUserIdIsNull(EMAIL, 1L)).thenReturn(Optional.empty());
		when(guardianRegistrationSettingsService.getMode(1L)).thenReturn(GuardianSelfRegistrationMode.CLAIM_ONLY);

		assertThrows(BusinessException.class,
				() -> guardianSelfRegistrationService.register(EMAIL, "Password123!", "Jane", "Doe", "555-0100"));

		verify(userService, never()).createUser(any(), any(), any(), any(), any(), any());
	}

	@Test
	void register_noPendingRecordAndOpenMode_createsZeroRoleUserAndNewGuardian() {
		when(guardianRepository.findByEmailAndTenantIdAndUserIdIsNull(EMAIL, 1L)).thenReturn(Optional.empty());
		when(guardianRegistrationSettingsService.getMode(1L)).thenReturn(GuardianSelfRegistrationMode.OPEN);
		User createdUser = userOf(200L);
		when(userService.createUser(eq(1L), eq(EMAIL), eq("Password123!"), eq("Jane"), eq("Doe"), eq(Set.of())))
				.thenReturn(createdUser);
		ArgumentCaptor<Guardian> guardianCaptor = ArgumentCaptor.forClass(Guardian.class);
		when(guardianRepository.save(guardianCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

		Guardian result = guardianSelfRegistrationService.register(EMAIL, "Password123!", "Jane", "Doe", "555-0100");

		assertEquals(200L, result.getUserId());
		assertEquals(EMAIL, guardianCaptor.getValue().getEmail());
	}

	private User userOf(Long id) {
		User user = User.builder().email(EMAIL).passwordHash("hash").firstName("Jane").lastName("Doe").build();
		user.setId(id);
		return user;
	}
}
