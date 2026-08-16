package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.subject.model.Subject;

/**
 * Verifies that subject records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class SubjectTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private SubjectService subjectService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "sub-a-" + suffix, 1L, "admin@sub-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "sub-b-" + suffix, 1L, "admin@sub-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void subjectCreatedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		subjectService.create("MATH-" + UUID.randomUUID().toString().substring(0, 6), "Mathematics", null);

		activateTenant(tenantB);
		Page<Subject> tenantBSubjects = subjectService.listSubjects(PageRequest.of(0, 100));

		boolean found = tenantBSubjects.getContent().stream()
				.anyMatch(s -> tenantA.getId().equals(s.getTenantId()));
		assertFalse(found, "Tenant B must not see subjects created under tenant A");
	}

	@Test
	void subjectPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		Subject subject = subjectService.create("SCI-" + UUID.randomUUID().toString().substring(0, 6), "Science",
				null);
		String publicId = subject.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> subjectService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's subject");
	}

	@Test
	void subjectCode_uniquenessIsScopedPerTenant() {
		String code = "ENG-" + UUID.randomUUID().toString().substring(0, 6);

		activateTenant(tenantA);
		subjectService.create(code, "English", null);

		activateTenant(tenantB);
		Subject tenantBSubject = subjectService.create(code, "English", null);

		assertNotNull(tenantBSubject, "Tenant B must be able to reuse a code already used by tenant A");
	}
}
