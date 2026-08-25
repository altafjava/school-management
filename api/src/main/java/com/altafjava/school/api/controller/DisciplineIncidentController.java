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
import com.altafjava.school.api.dto.request.RecordDisciplineActionRequest;
import com.altafjava.school.api.dto.request.RecordDisciplineIncidentRequest;
import com.altafjava.school.api.dto.response.DisciplineIncidentResponse;
import com.altafjava.school.api.mapper.DisciplineIncidentMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.DisciplineIncidentService;

@RestController
@RequestMapping("/api/v1/discipline-incidents")
public class DisciplineIncidentController {

	private final DisciplineIncidentService disciplineIncidentService;
	private final DisciplineIncidentMapper disciplineIncidentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public DisciplineIncidentController(DisciplineIncidentService disciplineIncidentService,
			DisciplineIncidentMapper disciplineIncidentMapper, SpringDataPageableResolver pageableResolver) {
		this.disciplineIncidentService = disciplineIncidentService;
		this.disciplineIncidentMapper = disciplineIncidentMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<DisciplineIncidentResponse> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return disciplineIncidentService.listAll(pageableResolver.resolve(page, size))
				.map(disciplineIncidentMapper::toResponse);
	}

	@GetMapping("/students/{studentPublicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<DisciplineIncidentResponse> listForStudent(@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return disciplineIncidentService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
				.map(disciplineIncidentMapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(SchoolRoles.HAS_TEACHER)
	public DisciplineIncidentResponse record(@Valid @RequestBody RecordDisciplineIncidentRequest request) {
		return disciplineIncidentMapper.toResponse(disciplineIncidentService.record(request.studentPublicId(),
				request.incidentDate(), request.severity(), request.description()));
	}

	@PatchMapping("/{publicId}/action")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public DisciplineIncidentResponse recordAction(@PathVariable String publicId,
			@Valid @RequestBody RecordDisciplineActionRequest request) {
		return disciplineIncidentMapper.toResponse(
				disciplineIncidentService.recordAction(publicId, request.actionTaken()));
	}
}
