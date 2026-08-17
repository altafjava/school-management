package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.DecideAdmissionRequest;
import com.altafjava.school.api.dto.request.SubmitAdmissionRequest;
import com.altafjava.school.api.dto.response.AdmissionResponse;
import com.altafjava.school.api.mapper.AdmissionMapper;
import com.altafjava.school.application.service.AdmissionService;

@RestController
@RequestMapping("/api/v1/admissions")
@PreAuthorize(Roles.HAS_TENANT_ADMIN)
public class AdmissionController {

	private final AdmissionService admissionService;
	private final AdmissionMapper admissionMapper;

	public AdmissionController(AdmissionService admissionService, AdmissionMapper admissionMapper) {
		this.admissionService = admissionService;
		this.admissionMapper = admissionMapper;
	}

	@GetMapping
	public Page<AdmissionResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return admissionService.listAdmissions(PageRequest.of(page, Math.min(size, 100)))
				.map(admissionMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	public AdmissionResponse get(@PathVariable String publicId) {
		return admissionMapper.toResponse(admissionService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AdmissionResponse submit(@Valid @RequestBody SubmitAdmissionRequest request) {
		return admissionMapper.toResponse(admissionService.submit(
				request.applicantFirstName(),
				request.applicantLastName(),
				request.applicantDateOfBirth(),
				request.guardianFirstName(),
				request.guardianLastName(),
				request.guardianEmail(),
				request.guardianPhone(),
				request.appliedGrade()));
	}

	@PatchMapping("/{publicId}/under-review")
	public AdmissionResponse markUnderReview(@PathVariable String publicId) {
		return admissionMapper.toResponse(admissionService.markUnderReview(publicId));
	}

	@PatchMapping("/{publicId}/decision")
	public AdmissionResponse decide(@PathVariable String publicId, @Valid @RequestBody DecideAdmissionRequest request) {
		return admissionMapper.toResponse(admissionService.decide(
				publicId,
				request.outcome(),
				request.decidedBy(),
				request.notes(),
				request.studentCode()));
	}
}
