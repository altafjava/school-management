package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.request.UpdateGuardianRegistrationSettingsRequest;
import com.altafjava.school.api.dto.response.GuardianRegistrationSettingsResponse;
import com.altafjava.school.application.service.GuardianRegistrationSettingsService;
import lombok.RequiredArgsConstructor;

// The school's own admin decides whether self-registration runs CLAIM_ONLY or OPEN — no other
// role involved, matching GradingScaleController's admin-only PUT restriction exactly.
@RestController
@RequestMapping("/api/v1/guardians/self-registration-settings")
@RequiredArgsConstructor
public class GuardianRegistrationSettingsController {

	private final GuardianRegistrationSettingsService guardianRegistrationSettingsService;

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_REGISTRATION_SETTINGS_MANAGE')")
	public GuardianRegistrationSettingsResponse get() {
		return new GuardianRegistrationSettingsResponse(guardianRegistrationSettingsService.getMode());
	}

	@PutMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_REGISTRATION_SETTINGS_MANAGE')")
	public GuardianRegistrationSettingsResponse update(
			@Valid @RequestBody UpdateGuardianRegistrationSettingsRequest request) {
		guardianRegistrationSettingsService.setMode(request.mode());
		return new GuardianRegistrationSettingsResponse(request.mode());
	}
}
