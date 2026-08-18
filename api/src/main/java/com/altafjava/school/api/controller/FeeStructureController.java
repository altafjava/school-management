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
import com.altafjava.school.api.dto.request.AssignFeeStructureRequest;
import com.altafjava.school.api.dto.request.CreateFeeStructureRequest;
import com.altafjava.school.api.dto.request.ReviseFeeAmountRequest;
import com.altafjava.school.api.dto.response.FeeAssignmentResponse;
import com.altafjava.school.api.dto.response.FeeStructureResponse;
import com.altafjava.school.api.mapper.FeeAssignmentMapper;
import com.altafjava.school.api.mapper.FeeStructureMapper;
import com.altafjava.school.application.service.FeeAssignmentService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.domain.fee.model.FeeFrequency;

@RestController
@RequestMapping("/api/v1/fee-structures")
public class FeeStructureController {

	private final FeeStructureService feeStructureService;
	private final FeeStructureMapper feeStructureMapper;
	private final FeeAssignmentService feeAssignmentService;
	private final FeeAssignmentMapper feeAssignmentMapper;

	public FeeStructureController(FeeStructureService feeStructureService, FeeStructureMapper feeStructureMapper,
			FeeAssignmentService feeAssignmentService, FeeAssignmentMapper feeAssignmentMapper) {
		this.feeStructureService = feeStructureService;
		this.feeStructureMapper = feeStructureMapper;
		this.feeAssignmentService = feeAssignmentService;
		this.feeAssignmentMapper = feeAssignmentMapper;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<FeeStructureResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return feeStructureService.listFeeStructures(PageRequest.of(page, Math.min(size, 100)))
				.map(feeStructureMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public FeeStructureResponse get(@PathVariable String publicId) {
		return feeStructureMapper.toResponse(feeStructureService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public FeeStructureResponse create(@Valid @RequestBody CreateFeeStructureRequest request) {
		return feeStructureMapper.toResponse(feeStructureService.create(
				request.name(),
				request.amount(),
				FeeFrequency.valueOf(request.frequency()),
				request.planType()));
	}

	@PatchMapping("/{publicId}/amount")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public FeeStructureResponse reviseAmount(@PathVariable String publicId,
			@Valid @RequestBody ReviseFeeAmountRequest request) {
		return feeStructureMapper.toResponse(feeStructureService.reviseAmount(publicId, request.amount()));
	}

	@PostMapping("/{publicId}/assignments")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public FeeAssignmentResponse assign(@PathVariable String publicId,
			@Valid @RequestBody AssignFeeStructureRequest request) {
		return feeAssignmentMapper.toResponse(
				feeAssignmentService.assign(publicId, request.studentPublicId(), request.classroomPublicId()));
	}

	@GetMapping("/{publicId}/assignments")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<FeeAssignmentResponse> listAssignments(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return feeAssignmentService.listForFeeStructure(publicId, PageRequest.of(page, Math.min(size, 100)))
				.map(feeAssignmentMapper::toResponse);
	}

	@PatchMapping("/{publicId}/assignments/{assignmentPublicId}/revoke")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public void revokeAssignment(@PathVariable String publicId, @PathVariable String assignmentPublicId) {
		feeAssignmentService.revoke(assignmentPublicId);
	}
}
