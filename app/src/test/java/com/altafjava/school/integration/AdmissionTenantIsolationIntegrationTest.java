package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
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
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.admission.model.Admission;

/**
 * Verifies that admission records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class AdmissionTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private AdmissionService admissionService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "adm-a-" + suffix, 1L, "admin@adm-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "adm-b-" + suffix, 1L, "admin@adm-b.test", "Password123!", "USD"));
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
	void admissionSubmittedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		admissionService.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith", "bob@family.test",
				"555-1234", "Grade 3");

		activateTenant(tenantB);
		Page<Admission> tenantBAdmissions = admissionService.listAdmissions(PageRequest.of(0, 100));

		boolean found = tenantBAdmissions.getContent().stream()
				.anyMatch(a -> tenantA.getId().equals(a.getTenantId()));
		assertFalse(found, "Tenant B must not see admissions submitted under tenant A");
	}

	@Test
	void admissionPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		Admission admission = admissionService.submit("Carol", "White", LocalDate.of(2014, 6, 1), "Dave", "White",
				"dave@family.test", "555-5678", "Grade 4");
		String publicId = admission.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> admissionService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's admission");
	}
}
