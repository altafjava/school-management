package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.CreateGuardianRequest;
import com.altafjava.school.api.dto.request.LinkGuardianRequest;
import com.altafjava.school.api.dto.response.GuardianResponse;
import com.altafjava.school.api.dto.response.StudentGuardianLinkResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.mapper.GuardianMapper;
import com.altafjava.school.api.mapper.StudentGuardianLinkMapper;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.GuardianService;

@RestController
@RequestMapping("/api/v1/guardians")
public class GuardianController {

	private final GuardianService guardianService;
	private final GuardianMapper guardianMapper;
	private final StudentGuardianLinkMapper studentGuardianLinkMapper;
	private final StudentMapper studentMapper;

	public GuardianController(GuardianService guardianService, GuardianMapper guardianMapper,
			StudentGuardianLinkMapper studentGuardianLinkMapper, StudentMapper studentMapper) {
		this.guardianService = guardianService;
		this.guardianMapper = guardianMapper;
		this.studentGuardianLinkMapper = studentGuardianLinkMapper;
		this.studentMapper = studentMapper;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<GuardianResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return guardianService.listGuardians(PageRequest.of(page, Math.min(size, 100)))
				.map(guardianMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GuardianResponse get(@PathVariable String publicId) {
		return guardianMapper.toResponse(guardianService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GuardianResponse create(@Valid @RequestBody CreateGuardianRequest request) {
		return guardianMapper.toResponse(guardianService.create(
				request.firstName(),
				request.lastName(),
				request.email(),
				request.phone(),
				request.userId()));
	}

	@PostMapping("/{publicId}/students")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public StudentGuardianLinkResponse linkStudent(@PathVariable String publicId,
			@Valid @RequestBody LinkGuardianRequest request) {
		return studentGuardianLinkMapper.toResponse(guardianService.linkToStudent(
				publicId,
				request.studentPublicId(),
				request.relationshipType(),
				request.primaryContact()));
	}

	@GetMapping("/me/students")
	@PreAuthorize(SchoolRoles.HAS_PARENT)
	public Page<StudentResponse> myStudents(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return guardianService.listLinkedStudentsForCurrentUser(PageRequest.of(page, Math.min(size, 100)))
				.map(studentMapper::toResponse);
	}
}
