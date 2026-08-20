package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

@ExtendWith(MockitoExtension.class)
class AttendanceAlertSettingsServiceTest {

	@Mock
	private TenantSettingOverrideService tenantSettingOverrideService;

	private AttendanceAlertSettingsService attendanceAlertSettingsService;

	@BeforeEach
	void setUp() {
		attendanceAlertSettingsService = new AttendanceAlertSettingsService(tenantSettingOverrideService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void getLowThresholdPercent_noOverrideConfigured_returnsDefault() {
		when(tenantSettingOverrideService.get(1L, AttendanceAlertSettingsService.SETTING_KEY))
				.thenReturn(Optional.empty());

		int threshold = attendanceAlertSettingsService.getLowThresholdPercent();

		assertEquals(AttendanceAlertSettingsService.DEFAULT_THRESHOLD_PERCENT, threshold);
	}

	@Test
	void getLowThresholdPercent_malformedValue_failsSafeToDefault() {
		when(tenantSettingOverrideService.get(1L, AttendanceAlertSettingsService.SETTING_KEY))
				.thenReturn(Optional.of("not-a-number"));

		int threshold = attendanceAlertSettingsService.getLowThresholdPercent();

		assertEquals(AttendanceAlertSettingsService.DEFAULT_THRESHOLD_PERCENT, threshold);
	}

	@Test
	void getLowThresholdPercent_validOverride_returnsParsedValue() {
		when(tenantSettingOverrideService.get(1L, AttendanceAlertSettingsService.SETTING_KEY))
				.thenReturn(Optional.of("60"));

		int threshold = attendanceAlertSettingsService.getLowThresholdPercent();

		assertEquals(60, threshold);
	}

	@Test
	void updateLowThresholdPercent_validValue_writesThroughSettingService() {
		attendanceAlertSettingsService.updateLowThresholdPercent(80);

		verify(tenantSettingOverrideService).set(eq(1L), eq(AttendanceAlertSettingsService.SETTING_KEY), eq("80"));
	}

	@Test
	void updateLowThresholdPercent_outOfRange_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> attendanceAlertSettingsService.updateLowThresholdPercent(101));
		assertThrows(IllegalArgumentException.class,
				() -> attendanceAlertSettingsService.updateLowThresholdPercent(-1));
	}
}
