package com.altafjava.school.api.controller;

import java.util.List;
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
import com.altafjava.school.api.dto.request.ConfigureLeaveCarryForwardRequest;
import com.altafjava.school.api.dto.request.CreateLeaveTypeRequest;
import com.altafjava.school.api.dto.request.UpdateLeaveTypeRequest;
import com.altafjava.school.api.dto.response.LeaveTypeResponse;
import com.altafjava.school.api.mapper.LeaveTypeMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.LeaveTypeService;

@RestController
@RequestMapping("/api/v1/leave-types")
public class LeaveTypeController {

	private final LeaveTypeService leaveTypeService;
	private final LeaveTypeMapper leaveTypeMapper;

	private final SpringDataPageableResolver pageableResolver;

	public LeaveTypeController(LeaveTypeService leaveTypeService, LeaveTypeMapper leaveTypeMapper,
			SpringDataPageableResolver pageableResolver) {
		this.leaveTypeService = leaveTypeService;
		this.leaveTypeMapper = leaveTypeMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_READ')")
	public Page<LeaveTypeResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return leaveTypeService.list(pageableResolver.resolve(page, size)).map(leaveTypeMapper::toResponse);
	}

	@GetMapping("/active")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_READ')")
	public List<LeaveTypeResponse> listActive() {
		return leaveTypeService.listActive().stream().map(leaveTypeMapper::toResponse).toList();
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_READ')")
	public LeaveTypeResponse get(@PathVariable String publicId) {
		return leaveTypeMapper.toResponse(leaveTypeService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public LeaveTypeResponse create(@Valid @RequestBody CreateLeaveTypeRequest request) {
		return leaveTypeMapper.toResponse(leaveTypeService.create(request.name(), request.defaultAnnualDays()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public LeaveTypeResponse updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateLeaveTypeRequest request) {
		return leaveTypeMapper
				.toResponse(leaveTypeService.updateDetails(publicId, request.name(), request.defaultAnnualDays()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public LeaveTypeResponse deactivate(@PathVariable String publicId) {
		return leaveTypeMapper.toResponse(leaveTypeService.deactivate(publicId));
	}

	// Drives PayrollCalculator's loss-of-pay basis (see PayslipService) — schools that want a leave
	// category excluded from pay mark it here rather than payroll inventing its own catalog.
	@PatchMapping("/{publicId}/mark-unpaid")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public LeaveTypeResponse markUnpaid(@PathVariable String publicId) {
		return leaveTypeMapper.toResponse(leaveTypeService.markUnpaid(publicId));
	}

	@PatchMapping("/{publicId}/mark-paid")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public LeaveTypeResponse markPaid(@PathVariable String publicId) {
		return leaveTypeMapper.toResponse(leaveTypeService.markPaid(publicId));
	}

	@PatchMapping("/{publicId}/restrict-during-probation")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public LeaveTypeResponse restrictDuringProbation(@PathVariable String publicId) {
		return leaveTypeMapper.toResponse(leaveTypeService.restrictDuringProbation(publicId));
	}

	@PatchMapping("/{publicId}/allow-during-probation")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public LeaveTypeResponse allowDuringProbation(@PathVariable String publicId) {
		return leaveTypeMapper.toResponse(leaveTypeService.allowDuringProbation(publicId));
	}

	@PatchMapping("/{publicId}/carry-forward")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public LeaveTypeResponse configureCarryForward(@PathVariable String publicId,
			@Valid @RequestBody ConfigureLeaveCarryForwardRequest request) {
		return leaveTypeMapper.toResponse(leaveTypeService.configureCarryForward(publicId, request.enabled(),
				request.maxCarryForwardDays(), request.carryForwardExpiryMonths()));
	}
}
