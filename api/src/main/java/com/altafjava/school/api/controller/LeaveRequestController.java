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
import com.altafjava.school.api.dto.request.RejectLeaveRequestRequest;
import com.altafjava.school.api.dto.request.SubmitLeaveRequestRequest;
import com.altafjava.school.api.dto.response.LeaveRequestResponse;
import com.altafjava.school.api.mapper.LeaveRequestMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.LeaveRequestService;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController {

	private final LeaveRequestService leaveRequestService;
	private final LeaveRequestMapper leaveRequestMapper;

	private final SpringDataPageableResolver pageableResolver;

	public LeaveRequestController(LeaveRequestService leaveRequestService, LeaveRequestMapper leaveRequestMapper,
			SpringDataPageableResolver pageableResolver) {
		this.leaveRequestService = leaveRequestService;
		this.leaveRequestMapper = leaveRequestMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<LeaveRequestResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return leaveRequestService.listAll(pageableResolver.resolve(page, size))
				.map(leaveRequestMapper::toResponse);
	}

	@GetMapping("/my")
	@PreAuthorize(SchoolRoles.HAS_TEACHER)
	public Page<LeaveRequestResponse> listMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return leaveRequestService.listForCurrentTeacher(pageableResolver.resolve(page, size))
				.map(leaveRequestMapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(SchoolRoles.HAS_TEACHER)
	public LeaveRequestResponse submit(@Valid @RequestBody SubmitLeaveRequestRequest request) {
		return leaveRequestMapper.toResponse(leaveRequestService.submit(request.leaveTypePublicId(),
				request.startDate(), request.endDate(), request.reason()));
	}

	@PatchMapping("/{publicId}/approve")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public LeaveRequestResponse approve(@PathVariable String publicId) {
		return leaveRequestMapper.toResponse(leaveRequestService.approve(publicId));
	}

	@PatchMapping("/{publicId}/reject")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public LeaveRequestResponse reject(@PathVariable String publicId,
			@Valid @RequestBody RejectLeaveRequestRequest request) {
		return leaveRequestMapper.toResponse(leaveRequestService.reject(publicId, request.rejectionReason()));
	}

	@PatchMapping("/{publicId}/cancel")
	@PreAuthorize(SchoolRoles.HAS_TEACHER)
	public LeaveRequestResponse cancel(@PathVariable String publicId) {
		return leaveRequestMapper.toResponse(leaveRequestService.cancel(publicId));
	}
}
