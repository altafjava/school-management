package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.request.UpsertHealthRecordRequest;
import com.altafjava.school.api.dto.response.HealthRecordResponse;
import com.altafjava.school.api.mapper.HealthRecordMapper;
import com.altafjava.school.application.service.HealthRecordService;

/**
 * Gated to {@code Roles.HAS_TENANT_ADMIN} only — no separate school/health-staff role
 * ("Nurse"/"HealthStaff") exists in the seeded role catalog, and this module does not invent new
 * platform-level RBAC infrastructure to add one. Health data is more sensitive than ordinary
 * operational data (see {@code HealthRecord}/{@code MedicalIncident} {@code @Pii} fields), so
 * unlike Transport/Hostel this intentionally excludes {@code TEACHER} from read access. A dedicated
 * health-staff role is a follow-up.
 */
@RestController
@RequestMapping("/api/v1/health-records")
public class HealthRecordController {

	private final HealthRecordService healthRecordService;
	private final HealthRecordMapper healthRecordMapper;

	public HealthRecordController(HealthRecordService healthRecordService, HealthRecordMapper healthRecordMapper) {
		this.healthRecordService = healthRecordService;
		this.healthRecordMapper = healthRecordMapper;
	}

	@GetMapping("/students/{studentPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HEALTH_RECORD_MANAGE')")
	public HealthRecordResponse getByStudent(@PathVariable String studentPublicId) {
		return healthRecordMapper.toResponse(healthRecordService.getByStudent(studentPublicId));
	}

	@PutMapping("/students/{studentPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HEALTH_RECORD_MANAGE')")
	public HealthRecordResponse upsert(@PathVariable String studentPublicId,
			@Valid @RequestBody UpsertHealthRecordRequest request) {
		return healthRecordMapper.toResponse(healthRecordService.upsert(studentPublicId, request.bloodGroup(),
				request.allergies(), request.conditions(), request.immunizations()));
	}
}
