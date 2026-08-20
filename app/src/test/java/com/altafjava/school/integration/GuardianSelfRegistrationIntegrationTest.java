package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.GuardianRegistrationSettingsService;
import com.altafjava.school.application.service.GuardianSelfRegistrationService;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.GuardianSelfRegistrationMode;

/**
 * Real-DB coverage of the dual self-registration mode: claim-existing-record vs. open-create-new,
 * and that a TENANT_ADMIN's setting change (via {@link GuardianRegistrationSettingsService}) takes
 * effect on the very next self-register call — no restart, no redeploy, just the DB write.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class GuardianSelfRegistrationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private GuardianSelfRegistrationService guardianSelfRegistrationService;

	@Autowired
	private GuardianRegistrationSettingsService guardianRegistrationSettingsService;

	@Autowired
	private GuardianService guardianService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenant;

	@BeforeEach
	void createTenant() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Guardian Self-Reg School", "grd-sr-" + suffix, 1L, "admin@grd-sr.test", "Password123!", "USD"));
		activate();
	}

	private void activate() {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void register_withPendingGuardianRecord_claimsRegardlessOfMode() {
		String email = "pending-" + UUID.randomUUID() + "@school.test";
		Guardian pending = guardianService.create("Jane", "Doe", email, "555-0100", null);

		Guardian claimed = guardianSelfRegistrationService.register(email, "Password123!", "Jane", "Doe", "555-0100");

		assertEquals(pending.getPublicId(), claimed.getPublicId());
		assertNotNull(claimed.getUserId());
	}

	@Test
	void register_noPendingRecord_defaultClaimOnlyMode_rejectsRegistration() {
		String email = "unmatched-" + UUID.randomUUID() + "@school.test";

		assertThrows(BusinessException.class,
				() -> guardianSelfRegistrationService.register(email, "Password123!", "Alex", "Roe", "555-0200"));
	}

	@Test
	void register_noPendingRecord_openMode_createsNewZeroRoleGuardian() {
		guardianRegistrationSettingsService.setMode(GuardianSelfRegistrationMode.OPEN);
		String email = "open-" + UUID.randomUUID() + "@school.test";

		Guardian created = guardianSelfRegistrationService.register(email, "Password123!", "Alex", "Roe",
				"555-0200");

		assertNotNull(created.getUserId());
		assertEquals(email, created.getEmail());
	}

	@Test
	void toggleMode_takesEffectOnNextRegisterCall_noRestartNeeded() {
		String email = "toggle-" + UUID.randomUUID() + "@school.test";

		assertThrows(BusinessException.class,
				() -> guardianSelfRegistrationService.register(email, "Password123!", "Sam", "Lee", "555-0300"));

		guardianRegistrationSettingsService.setMode(GuardianSelfRegistrationMode.OPEN);

		Guardian created = guardianSelfRegistrationService.register(email, "Password123!", "Sam", "Lee", "555-0300");
		assertNotNull(created.getUserId());
	}
}
