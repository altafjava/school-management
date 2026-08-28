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
import com.altafjava.school.api.dto.request.AssignTransportRequest;
import com.altafjava.school.api.dto.request.EndTransportAssignmentRequest;
import com.altafjava.school.api.dto.response.TransportAssignmentResponse;
import com.altafjava.school.api.mapper.TransportAssignmentMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.TransportAssignmentService;

@RestController
@RequestMapping("/api/v1/transport-assignments")
public class TransportAssignmentController {

	private final TransportAssignmentService transportAssignmentService;
	private final TransportAssignmentMapper transportAssignmentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public TransportAssignmentController(TransportAssignmentService transportAssignmentService,
			TransportAssignmentMapper transportAssignmentMapper, SpringDataPageableResolver pageableResolver) {
		this.transportAssignmentService = transportAssignmentService;
		this.transportAssignmentMapper = transportAssignmentMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ASSIGNMENT_READ')")
	public Page<TransportAssignmentResponse> listForRoute(
			@RequestParam String routePublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return transportAssignmentService.listForRoute(routePublicId, pageableResolver.resolve(page, size))
				.map(transportAssignmentMapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ASSIGNMENT_WRITE')")
	public TransportAssignmentResponse assign(@Valid @RequestBody AssignTransportRequest request) {
		return transportAssignmentMapper.toResponse(transportAssignmentService.assign(request.studentPublicId(),
				request.routePublicId(), request.vehiclePublicId(), request.routeStopPublicId(),
				request.effectiveFrom()));
	}

	@PatchMapping("/{publicId}/end")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ASSIGNMENT_WRITE')")
	public TransportAssignmentResponse end(@PathVariable String publicId,
			@Valid @RequestBody EndTransportAssignmentRequest request) {
		return transportAssignmentMapper.toResponse(transportAssignmentService.end(publicId, request.effectiveTo()));
	}
}
