package com.altafjava.school.application.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.guardian.model.GuardianSelfRegistrationMode;
import lombok.RequiredArgsConstructor;

// Bridges GuardianSelfRegistrationMode to TenantSettingOverride; tenants without one get CLAIM_ONLY.
// Mirrors GradingScaleService's/AttendanceAlertSettingsService's tenant-owned-setting shape exactly
// — this is the school admin's own operational choice, not SUPER_ADMIN entitlement gating, so it
// deliberately does not use the platform FeatureFlag/TenantFeatureOverride mechanism.
@Service
@RequiredArgsConstructor
public class GuardianRegistrationSettingsService {

	static final String SETTING_KEY = "school.guardian.self-registration-mode";

	private final TenantSettingOverrideService tenantSettingOverrideService;

	@Transactional(readOnly = true)
	public GuardianSelfRegistrationMode getMode() {
		return getMode(TenantContext.getCurrentTenantId());
	}

	// Absent key or an unparseable stored value both fail safe to CLAIM_ONLY (the stricter mode) —
	// never fail-open into OPEN, which would let self-registration mint brand-new guardian rows.
	@Transactional(readOnly = true)
	public GuardianSelfRegistrationMode getMode(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, SETTING_KEY)
				.flatMap(this::parse)
				.orElse(GuardianSelfRegistrationMode.CLAIM_ONLY);
	}

	@Transactional
	public void setMode(GuardianSelfRegistrationMode mode) {
		setMode(TenantContext.getCurrentTenantId(), mode);
	}

	@Transactional
	public void setMode(Long tenantId, GuardianSelfRegistrationMode mode) {
		tenantSettingOverrideService.set(tenantId, SETTING_KEY, mode.name());
	}

	private Optional<GuardianSelfRegistrationMode> parse(String value) {
		try {
			return Optional.of(GuardianSelfRegistrationMode.valueOf(value));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
