package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.request.RecordMedicalIncidentRequest;
import com.altafjava.school.api.dto.response.MedicalIncidentResponse;
import com.altafjava.school.api.mapper.MedicalIncidentMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.MedicalIncidentService;

// See HealthRecordController for the TENANT_ADMIN-only rationale (PHI-grade data, no dedicated
// health-staff role in the seeded catalog).
@RestController
@RequestMapping("/api/v1/medical-incidents")
public class MedicalIncidentController {

	private final MedicalIncidentService medicalIncidentService;
	private final MedicalIncidentMapper medicalIncidentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public MedicalIncidentController(MedicalIncidentService medicalIncidentService,
			MedicalIncidentMapper medicalIncidentMapper, SpringDataPageableResolver pageableResolver) {
		this.medicalIncidentService = medicalIncidentService;
		this.medicalIncidentMapper = medicalIncidentMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('MEDICAL_INCIDENT_MANAGE')")
	public Page<MedicalIncidentResponse> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return medicalIncidentService.listAll(pageableResolver.resolve(page, size))
				.map(medicalIncidentMapper::toResponse);
	}

	@GetMapping("/students/{studentPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('MEDICAL_INCIDENT_MANAGE')")
	public Page<MedicalIncidentResponse> listForStudent(@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return medicalIncidentService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
				.map(medicalIncidentMapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('MEDICAL_INCIDENT_MANAGE')")
	public MedicalIncidentResponse record(@Valid @RequestBody RecordMedicalIncidentRequest request) {
		return medicalIncidentMapper.toResponse(medicalIncidentService.record(request.studentPublicId(),
				request.occurredAt(), request.description(), request.treatmentGiven()));
	}
}
