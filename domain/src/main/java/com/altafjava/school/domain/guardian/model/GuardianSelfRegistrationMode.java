package com.altafjava.school.domain.guardian.model;

// Tenant-wide setting, not a Guardian field — stored via TenantSettingOverride
// (see GuardianRegistrationSettingsService), so this is a plain enum, not JPA-mapped.
public enum GuardianSelfRegistrationMode {

	// Self-registration only succeeds by claiming an existing, admin-created, unclaimed guardian
	// record matched by email. The safe default.
	CLAIM_ONLY,

	// Self-registration also creates a brand-new Guardian row (with zero platform roles) when no
	// matching unclaimed record exists.
	OPEN
}
