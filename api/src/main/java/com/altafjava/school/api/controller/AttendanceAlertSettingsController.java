package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.UpdateLowAttendanceThresholdRequest;
import com.altafjava.school.api.dto.response.LowAttendanceThresholdResponse;
import com.altafjava.school.application.service.AttendanceAlertSettingsService;

@RestController
@RequestMapping("/api/v1/attendance/low-threshold-setting")
public class AttendanceAlertSettingsController {

	private final AttendanceAlertSettingsService attendanceAlertSettingsService;

	public AttendanceAlertSettingsController(AttendanceAlertSettingsService attendanceAlertSettingsService) {
		this.attendanceAlertSettingsService = attendanceAlertSettingsService;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public LowAttendanceThresholdResponse get() {
		return new LowAttendanceThresholdResponse(attendanceAlertSettingsService.getLowThresholdPercent());
	}

	@PutMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public LowAttendanceThresholdResponse update(@Valid @RequestBody UpdateLowAttendanceThresholdRequest request) {
		return new LowAttendanceThresholdResponse(
				attendanceAlertSettingsService.updateLowThresholdPercent(request.thresholdPercent()));
	}
}
