package com.altafjava.school.api.controller;

import java.util.List;
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
import com.altafjava.school.api.dto.request.CreateLeaveTypeRequest;
import com.altafjava.school.api.dto.request.UpdateLeaveTypeRequest;
import com.altafjava.school.api.dto.response.LeaveTypeResponse;
import com.altafjava.school.api.mapper.LeaveTypeMapper;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.LeaveTypeService;

@RestController
@RequestMapping("/api/v1/leave-types")
public class LeaveTypeController {

	private final LeaveTypeService leaveTypeService;
	private final LeaveTypeMapper leaveTypeMapper;

	public LeaveTypeController(LeaveTypeService leaveTypeService, LeaveTypeMapper leaveTypeMapper) {
		this.leaveTypeService = leaveTypeService;
		this.leaveTypeMapper = leaveTypeMapper;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<LeaveTypeResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return leaveTypeService.list(PageRequest.of(page, Math.min(size, 100))).map(leaveTypeMapper::toResponse);
	}

	@GetMapping("/active")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public List<LeaveTypeResponse> listActive() {
		return leaveTypeService.listActive().stream().map(leaveTypeMapper::toResponse).toList();
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public LeaveTypeResponse get(@PathVariable String publicId) {
		return leaveTypeMapper.toResponse(leaveTypeService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public LeaveTypeResponse create(@Valid @RequestBody CreateLeaveTypeRequest request) {
		return leaveTypeMapper.toResponse(leaveTypeService.create(request.name(), request.defaultAnnualDays()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public LeaveTypeResponse updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateLeaveTypeRequest request) {
		return leaveTypeMapper
				.toResponse(leaveTypeService.updateDetails(publicId, request.name(), request.defaultAnnualDays()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public LeaveTypeResponse deactivate(@PathVariable String publicId) {
		return leaveTypeMapper.toResponse(leaveTypeService.deactivate(publicId));
	}
}
