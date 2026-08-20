package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.guardian.model.GuardianSelfRegistrationMode;

@ExtendWith(MockitoExtension.class)
class GuardianRegistrationSettingsServiceTest {

	@Mock
	private TenantSettingOverrideService tenantSettingOverrideService;

	private GuardianRegistrationSettingsService guardianRegistrationSettingsService;

	@BeforeEach
	void setUp() {
		guardianRegistrationSettingsService = new GuardianRegistrationSettingsService(tenantSettingOverrideService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void getMode_noOverrideConfigured_defaultsToClaimOnly() {
		when(tenantSettingOverrideService.get(1L, GuardianRegistrationSettingsService.SETTING_KEY))
				.thenReturn(Optional.empty());

		assertEquals(GuardianSelfRegistrationMode.CLAIM_ONLY, guardianRegistrationSettingsService.getMode());
	}

	@Test
	void getMode_corruptStoredValue_failsSafeToClaimOnly() {
		when(tenantSettingOverrideService.get(1L, GuardianRegistrationSettingsService.SETTING_KEY))
				.thenReturn(Optional.of("NOT_A_REAL_MODE"));

		assertEquals(GuardianSelfRegistrationMode.CLAIM_ONLY, guardianRegistrationSettingsService.getMode());
	}

	@Test
	void getMode_validStoredValue_parsesToOpen() {
		when(tenantSettingOverrideService.get(1L, GuardianRegistrationSettingsService.SETTING_KEY))
				.thenReturn(Optional.of("OPEN"));

		assertEquals(GuardianSelfRegistrationMode.OPEN, guardianRegistrationSettingsService.getMode());
	}

	@Test
	void setMode_writesEnumNameThroughTenantSettingOverrideService() {
		guardianRegistrationSettingsService.setMode(GuardianSelfRegistrationMode.OPEN);

		verify(tenantSettingOverrideService).set(eq(1L), eq(GuardianRegistrationSettingsService.SETTING_KEY),
				eq("OPEN"));
	}

	@Test
	void getMode_withExplicitTenantId_readsThatTenantRegardlessOfCurrentContext() {
		when(tenantSettingOverrideService.get(2L, GuardianRegistrationSettingsService.SETTING_KEY))
				.thenReturn(Optional.of("OPEN"));

		assertEquals(GuardianSelfRegistrationMode.OPEN, guardianRegistrationSettingsService.getMode(2L));
	}
}
