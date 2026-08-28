package com.altafjava.school.api.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.response.LeaveBalanceResponse;
import com.altafjava.school.api.mapper.LeaveBalanceMapper;
import com.altafjava.school.application.service.LeaveBalanceService;

@RestController
public class LeaveBalanceController {

	private final LeaveBalanceService leaveBalanceService;
	private final LeaveBalanceMapper leaveBalanceMapper;

	public LeaveBalanceController(LeaveBalanceService leaveBalanceService, LeaveBalanceMapper leaveBalanceMapper) {
		this.leaveBalanceService = leaveBalanceService;
		this.leaveBalanceMapper = leaveBalanceMapper;
	}

	@GetMapping("/api/v1/teachers/{publicId}/leave-balances")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_BALANCE_MANAGE')")
	public List<LeaveBalanceResponse> forTeacher(@PathVariable String publicId,
			@RequestParam String academicYearPublicId) {
		return leaveBalanceService.listForTeacher(publicId, academicYearPublicId).stream()
				.map(leaveBalanceMapper::toResponse)
				.toList();
	}

	@GetMapping("/api/v1/leave-requests/my/balances")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_SELF_SERVICE')")
	public List<LeaveBalanceResponse> forCurrentTeacher(@RequestParam String academicYearPublicId) {
		return leaveBalanceService.listForCurrentTeacher(academicYearPublicId).stream()
				.map(leaveBalanceMapper::toResponse)
				.toList();
	}
}
