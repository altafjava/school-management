package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.CounselingReferralService;
import com.altafjava.school.application.service.CounselingSessionService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.teacher.model.Teacher;

/**
 * Verifies that counseling sessions and referrals created under tenant A are not visible or
 * actionable from tenant B — this is PHI-grade confidential data ({@code @Pii}-flagged), so
 * isolation matters even more than usual.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class CounselingTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private CounselingSessionService counselingSessionService;

	@Autowired
	private CounselingReferralService counselingReferralService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"Counseling School A", "counseling-a-" + suffix, 1L, "admin@counseling-a.test", "Password123!",
				"USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Counseling School B", "counseling-b-" + suffix, 1L, "admin@counseling-b.test", "Password123!",
				"USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	@Test
	void counselingSessionCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice",
				"Smith", "alice@counseling.test", LocalDate.of(2010, 1, 1));
		Teacher counselor = teacherService.hire("EMP-" + UUID.randomUUID().toString().substring(0, 6), "Jane", "Doe",
				"jane@counseling.test", LocalDate.of(2020, 1, 1));
		var session = counselingSessionService.schedule(student.getPublicId().toString(),
				counselor.getPublicId().toString(), LocalDate.of(2026, 5, 1), "Discussed exam anxiety", true);
		String sessionPublicId = session.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> counselingSessionService.get(sessionPublicId),
				"Tenant B must not be able to resolve tenant A's counseling session");
	}

	@Test
	void counselingReferralCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		authenticateAsUser(55L);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Bob",
				"Jones", "bob@counseling.test", LocalDate.of(2011, 2, 2));
		var referral = counselingReferralService.refer(student.getPublicId().toString(), "Struggling academically");
		String studentPublicId = student.getPublicId().toString();
		String referralPublicId = referral.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> counselingReferralService.listForStudent(studentPublicId, PageRequest.of(0, 20)),
				"Tenant B must not resolve tenant A's student at all");
		assertThrows(ResourceNotFoundException.class, () -> counselingReferralService.get(referralPublicId),
				"Tenant B must not be able to resolve tenant A's counseling referral");
	}
}
