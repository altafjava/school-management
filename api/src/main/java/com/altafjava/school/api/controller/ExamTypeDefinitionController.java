package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.request.CreateExamTypeDefinitionRequest;
import com.altafjava.school.api.dto.request.UpdateExamTypeDefinitionRequest;
import com.altafjava.school.api.dto.response.ExamTypeDefinitionResponse;
import com.altafjava.school.api.mapper.ExamTypeDefinitionMapper;
import com.altafjava.school.application.service.ExamTypeDefinitionService;

@RestController
@RequestMapping("/api/v1/exam-type-definitions")
public class ExamTypeDefinitionController {

	private final ExamTypeDefinitionService examTypeDefinitionService;
	private final ExamTypeDefinitionMapper examTypeDefinitionMapper;

	public ExamTypeDefinitionController(ExamTypeDefinitionService examTypeDefinitionService,
			ExamTypeDefinitionMapper examTypeDefinitionMapper) {
		this.examTypeDefinitionService = examTypeDefinitionService;
		this.examTypeDefinitionMapper = examTypeDefinitionMapper;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_TYPE_MANAGE')")
	public List<ExamTypeDefinitionResponse> list() {
		return examTypeDefinitionService.list().stream().map(examTypeDefinitionMapper::toResponse).toList();
	}

	@GetMapping("/active")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_TYPE_MANAGE')")
	public List<ExamTypeDefinitionResponse> listActive() {
		return examTypeDefinitionService.listActive().stream().map(examTypeDefinitionMapper::toResponse).toList();
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_TYPE_MANAGE')")
	public ExamTypeDefinitionResponse get(@PathVariable String publicId) {
		return examTypeDefinitionMapper.toResponse(examTypeDefinitionService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_TYPE_MANAGE')")
	public ExamTypeDefinitionResponse create(@Valid @RequestBody CreateExamTypeDefinitionRequest request) {
		return examTypeDefinitionMapper
				.toResponse(examTypeDefinitionService.create(request.code(), request.name(), request.displayOrder()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_TYPE_MANAGE')")
	public ExamTypeDefinitionResponse update(@PathVariable String publicId,
			@Valid @RequestBody UpdateExamTypeDefinitionRequest request) {
		return examTypeDefinitionMapper.toResponse(
				examTypeDefinitionService.update(publicId, request.name(), request.active(), request.displayOrder()));
	}
}
