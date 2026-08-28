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
import com.altafjava.school.api.dto.request.CreateHostelBuildingRequest;
import com.altafjava.school.api.dto.request.UpdateHostelBuildingRequest;
import com.altafjava.school.api.dto.response.HostelBuildingResponse;
import com.altafjava.school.api.mapper.HostelBuildingMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.HostelBuildingService;

// No dedicated hostel-warden role exists in the seeded role catalog (mirrors Transport's
// TENANT_ADMIN-or-TEACHER gate for reads) — a warden-specific role is a follow-up.
@RestController
@RequestMapping("/api/v1/hostel-buildings")
public class HostelBuildingController {

	private final HostelBuildingService hostelBuildingService;
	private final HostelBuildingMapper hostelBuildingMapper;

	private final SpringDataPageableResolver pageableResolver;

	public HostelBuildingController(HostelBuildingService hostelBuildingService,
			HostelBuildingMapper hostelBuildingMapper, SpringDataPageableResolver pageableResolver) {
		this.hostelBuildingService = hostelBuildingService;
		this.hostelBuildingMapper = hostelBuildingMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOSTEL_READ')")
	public Page<HostelBuildingResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return hostelBuildingService.list(pageableResolver.resolve(page, size)).map(hostelBuildingMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOSTEL_READ')")
	public HostelBuildingResponse get(@PathVariable String publicId) {
		return hostelBuildingMapper.toResponse(hostelBuildingService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOSTEL_WRITE')")
	public HostelBuildingResponse create(@Valid @RequestBody CreateHostelBuildingRequest request) {
		return hostelBuildingMapper.toResponse(hostelBuildingService.create(request.name(), request.address()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOSTEL_WRITE')")
	public HostelBuildingResponse updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateHostelBuildingRequest request) {
		return hostelBuildingMapper.toResponse(
				hostelBuildingService.updateDetails(publicId, request.name(), request.address()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOSTEL_WRITE')")
	public HostelBuildingResponse deactivate(@PathVariable String publicId) {
		return hostelBuildingMapper.toResponse(hostelBuildingService.deactivate(publicId));
	}
}
