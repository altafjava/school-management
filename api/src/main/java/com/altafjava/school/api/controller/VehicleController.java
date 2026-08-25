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
import com.altafjava.school.api.dto.request.CreateVehicleRequest;
import com.altafjava.school.api.dto.request.UpdateVehicleRequest;
import com.altafjava.school.api.dto.response.VehicleResponse;
import com.altafjava.school.api.mapper.VehicleMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.VehicleService;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

	private final VehicleService vehicleService;
	private final VehicleMapper vehicleMapper;

	private final SpringDataPageableResolver pageableResolver;

	public VehicleController(VehicleService vehicleService, VehicleMapper vehicleMapper,
			SpringDataPageableResolver pageableResolver) {
		this.vehicleService = vehicleService;
		this.vehicleMapper = vehicleMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<VehicleResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return vehicleService.list(pageableResolver.resolve(page, size)).map(vehicleMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public VehicleResponse get(@PathVariable String publicId) {
		return vehicleMapper.toResponse(vehicleService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public VehicleResponse create(@Valid @RequestBody CreateVehicleRequest request) {
		return vehicleMapper.toResponse(vehicleService.create(request.registrationNumber(), request.capacity(),
				request.driverName(), request.driverContact()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public VehicleResponse updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateVehicleRequest request) {
		return vehicleMapper.toResponse(vehicleService.updateDetails(publicId, request.capacity(),
				request.driverName(), request.driverContact()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public VehicleResponse deactivate(@PathVariable String publicId) {
		return vehicleMapper.toResponse(vehicleService.deactivate(publicId));
	}
}
