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
import com.altafjava.school.api.dto.request.CreateSubjectRequest;
import com.altafjava.school.api.dto.response.SubjectResponse;
import com.altafjava.school.api.mapper.SubjectMapper;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.SubjectService;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

	private final SubjectService subjectService;
	private final SubjectMapper subjectMapper;

	public SubjectController(SubjectService subjectService, SubjectMapper subjectMapper) {
		this.subjectService = subjectService;
		this.subjectMapper = subjectMapper;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<SubjectResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return subjectService.listSubjects(PageRequest.of(page, Math.min(size, 100)))
				.map(subjectMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public SubjectResponse get(@PathVariable String publicId) {
		return subjectMapper.toResponse(subjectService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public SubjectResponse create(@Valid @RequestBody CreateSubjectRequest request) {
		return subjectMapper.toResponse(subjectService.create(
				request.code(),
				request.name(),
				request.description()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public SubjectResponse deactivate(@PathVariable String publicId) {
		return subjectMapper.toResponse(subjectService.deactivate(publicId));
	}
}
