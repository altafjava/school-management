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
import com.altafjava.school.api.dto.request.AssignHeadTeacherRequest;
import com.altafjava.school.api.dto.request.CreateDepartmentRequest;
import com.altafjava.school.api.dto.request.UpdateDepartmentRequest;
import com.altafjava.school.api.dto.response.DepartmentResponse;
import com.altafjava.school.api.mapper.DepartmentMapper;
import com.altafjava.school.application.service.DepartmentService;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

	private final DepartmentService departmentService;
	private final DepartmentMapper departmentMapper;

	public DepartmentController(DepartmentService departmentService, DepartmentMapper departmentMapper) {
		this.departmentService = departmentService;
		this.departmentMapper = departmentMapper;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<DepartmentResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return departmentService.list(PageRequest.of(page, Math.min(size, 100))).map(departmentMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public DepartmentResponse get(@PathVariable String publicId) {
		return departmentMapper.toResponse(departmentService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public DepartmentResponse create(@Valid @RequestBody CreateDepartmentRequest request) {
		return departmentMapper
				.toResponse(departmentService.create(request.name(), request.code(), request.description()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public DepartmentResponse updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateDepartmentRequest request) {
		return departmentMapper.toResponse(
				departmentService.updateDetails(publicId, request.name(), request.code(), request.description()));
	}

	@PatchMapping("/{publicId}/head-teacher")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public DepartmentResponse assignHeadTeacher(@PathVariable String publicId,
			@Valid @RequestBody AssignHeadTeacherRequest request) {
		return departmentMapper
				.toResponse(departmentService.assignHeadTeacher(publicId, request.headTeacherPublicId()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public DepartmentResponse deactivate(@PathVariable String publicId) {
		return departmentMapper.toResponse(departmentService.deactivate(publicId));
	}
}
