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
import com.altafjava.school.api.dto.request.CreateAssignmentRequest;
import com.altafjava.school.api.dto.request.RescheduleAssignmentRequest;
import com.altafjava.school.api.dto.response.AssignmentResponse;
import com.altafjava.school.api.mapper.AssignmentMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.AssignmentService;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

	private final AssignmentService assignmentService;
	private final AssignmentMapper assignmentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AssignmentController(AssignmentService assignmentService, AssignmentMapper assignmentMapper,
			SpringDataPageableResolver pageableResolver) {
		this.assignmentService = assignmentService;
		this.assignmentMapper = assignmentMapper;
		this.pageableResolver = pageableResolver;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(SchoolRoles.HAS_TEACHER)
	public AssignmentResponse create(@Valid @RequestBody CreateAssignmentRequest request) {
		return assignmentMapper.toResponse(assignmentService.create(
				request.classroomPublicId(),
				request.subjectPublicId(),
				request.title(),
				request.description(),
				request.storageKey(),
				request.dueDate(),
				request.maxMarks()));
	}

	@GetMapping("/classroom/{classroomPublicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<AssignmentResponse> listByClassroom(@PathVariable String classroomPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return assignmentService.listByClassroom(classroomPublicId, pageableResolver.resolve(page, size))
				.map(assignmentMapper::toResponse);
	}

	@PatchMapping("/{publicId}/reschedule")
	@PreAuthorize(SchoolRoles.HAS_TEACHER)
	public AssignmentResponse reschedule(@PathVariable String publicId,
			@Valid @RequestBody RescheduleAssignmentRequest request) {
		return assignmentMapper.toResponse(assignmentService.reschedule(publicId, request.dueDate()));
	}
}
