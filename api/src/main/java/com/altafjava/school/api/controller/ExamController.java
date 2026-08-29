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
import com.altafjava.school.api.dto.request.AssignExamTermRequest;
import com.altafjava.school.api.dto.request.RescheduleExamRequest;
import com.altafjava.school.api.dto.request.ScheduleExamRequest;
import com.altafjava.school.api.dto.response.ExamResponse;
import com.altafjava.school.api.mapper.ExamMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.ExamService;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

	private final ExamService examService;
	private final ExamMapper examMapper;

	private final SpringDataPageableResolver pageableResolver;

	public ExamController(ExamService examService, ExamMapper examMapper, SpringDataPageableResolver pageableResolver) {
		this.examService = examService;
		this.examMapper = examMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_COMPLETE')")
	public Page<ExamResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return examService.listExams(pageableResolver.resolve(page, size))
				.map(examMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_COMPLETE')")
	public ExamResponse get(@PathVariable String publicId) {
		return examMapper.toResponse(examService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ExamResponse schedule(@Valid @RequestBody ScheduleExamRequest request) {
		return examMapper.toResponse(examService.schedule(
				request.title(),
				request.subjectId(),
				request.classroomId(),
				request.scheduledAt(),
				request.maxMarks(),
				request.termId(),
				request.examTypeId()));
	}

	@PatchMapping("/{publicId}/schedule")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ExamResponse reschedule(@PathVariable String publicId, @Valid @RequestBody RescheduleExamRequest request) {
		return examMapper.toResponse(examService.reschedule(publicId, request.scheduledAt()));
	}

	@PatchMapping("/{publicId}/term")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ExamResponse assignTerm(@PathVariable String publicId, @Valid @RequestBody AssignExamTermRequest request) {
		return examMapper.toResponse(examService.assignTerm(publicId, request.termId()));
	}

	@PatchMapping("/{publicId}/complete")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_COMPLETE')")
	public ExamResponse complete(@PathVariable String publicId) {
		return examMapper.toResponse(examService.complete(publicId));
	}

	@PatchMapping("/{publicId}/cancel")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ExamResponse cancel(@PathVariable String publicId) {
		return examMapper.toResponse(examService.cancel(publicId));
	}
}
