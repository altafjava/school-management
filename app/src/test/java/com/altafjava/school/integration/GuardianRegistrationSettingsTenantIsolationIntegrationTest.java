package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.GuardianRegistrationSettingsService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.guardian.model.GuardianSelfRegistrationMode;

// Verifies tenant A's admin flipping self-registration mode does not affect tenant B.
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class GuardianRegistrationSettingsTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private GuardianRegistrationSettingsService guardianRegistrationSettingsService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "grs-a-" + suffix, 1L, "admin@grs-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "grs-b-" + suffix, 1L, "admin@grs-b.test", "Password123!", "USD"));
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
	void tenantAAdminSettingOpenMode_doesNotAffectTenantB() {
		activateTenant(tenantA);
		guardianRegistrationSettingsService.setMode(GuardianSelfRegistrationMode.OPEN);

		activateTenant(tenantB);
		assertEquals(GuardianSelfRegistrationMode.CLAIM_ONLY, guardianRegistrationSettingsService.getMode(),
				"Tenant B must still default to CLAIM_ONLY after tenant A opts into OPEN");

		activateTenant(tenantA);
		assertEquals(GuardianSelfRegistrationMode.OPEN, guardianRegistrationSettingsService.getMode(),
				"Tenant A's own setting must still read back as OPEN");
	}
}
