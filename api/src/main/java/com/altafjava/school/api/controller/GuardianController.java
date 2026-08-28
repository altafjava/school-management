package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
import com.altafjava.school.api.dto.request.AddressRequest;
import com.altafjava.school.api.dto.request.CreateGuardianRequest;
import com.altafjava.school.api.dto.request.LinkGuardianRequest;
import com.altafjava.school.api.dto.request.UpdatePhoneRequest;
import com.altafjava.school.api.dto.response.GuardianResponse;
import com.altafjava.school.api.dto.response.StudentGuardianLinkResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.mapper.AddressMapper;
import com.altafjava.school.api.mapper.GuardianMapper;
import com.altafjava.school.api.mapper.StudentGuardianLinkMapper;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.GuardianService;

@RestController
@RequestMapping("/api/v1/guardians")
public class GuardianController {

	private final GuardianService guardianService;
	private final GuardianMapper guardianMapper;
	private final AddressMapper addressMapper;
	private final StudentGuardianLinkMapper studentGuardianLinkMapper;
	private final StudentMapper studentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public GuardianController(GuardianService guardianService, GuardianMapper guardianMapper,
			AddressMapper addressMapper, StudentGuardianLinkMapper studentGuardianLinkMapper,
			StudentMapper studentMapper, SpringDataPageableResolver pageableResolver) {
		this.guardianService = guardianService;
		this.guardianMapper = guardianMapper;
		this.addressMapper = addressMapper;
		this.studentGuardianLinkMapper = studentGuardianLinkMapper;
		this.studentMapper = studentMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<GuardianResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return guardianService.listGuardians(pageableResolver.resolve(page, size))
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

	@PatchMapping("/{publicId}/address")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GuardianResponse updateAddress(@PathVariable String publicId, @Valid @RequestBody AddressRequest request) {
		return guardianMapper.toResponse(guardianService.updateAddress(publicId, addressMapper.toDomain(request)));
	}

	@PatchMapping("/{publicId}/phone")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GuardianResponse updatePhone(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhoneRequest request) {
		return guardianMapper.toResponse(guardianService.updatePhone(publicId, request.phone()));
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

	@PatchMapping("/{guardianPublicId}/students/{studentPublicId}/consent/grant")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_PARENT)
	public StudentGuardianLinkResponse grantConsent(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId) {
		return studentGuardianLinkMapper.toResponse(
				guardianService.grantConsent(guardianPublicId, studentPublicId));
	}

	@PatchMapping("/{guardianPublicId}/students/{studentPublicId}/consent/revoke")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_PARENT)
	public StudentGuardianLinkResponse revokeConsent(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId) {
		return studentGuardianLinkMapper.toResponse(
				guardianService.revokeConsent(guardianPublicId, studentPublicId));
	}

	@GetMapping("/me/students")
	@PreAuthorize(SchoolRoles.HAS_PARENT)
	public Page<StudentResponse> myStudents(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return guardianService.listLinkedStudentsForCurrentUser(pageableResolver.resolve(page, size))
				.map(studentMapper::toResponse);
	}
}
