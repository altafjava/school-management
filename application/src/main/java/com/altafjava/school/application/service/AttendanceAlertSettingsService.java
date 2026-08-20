package com.altafjava.school.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;

// Bridges the low-attendance alert threshold to TenantSettingOverride; tenants without one get the default.
@Service
@RequiredArgsConstructor
public class AttendanceAlertSettingsService {

	static final String SETTING_KEY = "school.attendance.low-threshold-percent";
	static final int DEFAULT_THRESHOLD_PERCENT = 75;

	private final TenantSettingOverrideService tenantSettingOverrideService;

	@Transactional(readOnly = true)
	public int getLowThresholdPercent() {
		return getLowThresholdPercent(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public int getLowThresholdPercent(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, SETTING_KEY)
				.map(this::parse)
				.orElse(DEFAULT_THRESHOLD_PERCENT);
	}

	@Transactional
	public int updateLowThresholdPercent(int thresholdPercent) {
		if (thresholdPercent < 0 || thresholdPercent > 100) {
			throw new IllegalArgumentException("thresholdPercent must be between 0 and 100");
		}
		Long tenantId = TenantContext.getCurrentTenantId();
		tenantSettingOverrideService.set(tenantId, SETTING_KEY, String.valueOf(thresholdPercent));
		return thresholdPercent;
	}

	// Malformed setting values fail safe to the default rather than breaking the alert job.
	private int parse(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return DEFAULT_THRESHOLD_PERCENT;
		}
	}
}
