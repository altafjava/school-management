package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.request.PostLessonRequest;
import com.altafjava.school.api.dto.response.LessonResponse;
import com.altafjava.school.api.mapper.LessonMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.LessonService;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {

	private final LessonService lessonService;
	private final LessonMapper lessonMapper;

	private final SpringDataPageableResolver pageableResolver;

	public LessonController(LessonService lessonService, LessonMapper lessonMapper,
			SpringDataPageableResolver pageableResolver) {
		this.lessonService = lessonService;
		this.lessonMapper = lessonMapper;
		this.pageableResolver = pageableResolver;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(SchoolRoles.HAS_TEACHER)
	public LessonResponse post(@Valid @RequestBody PostLessonRequest request) {
		return lessonMapper.toResponse(lessonService.post(
				request.classroomPublicId(),
				request.subjectPublicId(),
				request.title(),
				request.description(),
				request.storageKey()));
	}

	@GetMapping("/classroom/{classroomPublicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<LessonResponse> listByClassroom(@PathVariable String classroomPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return lessonService.listByClassroom(classroomPublicId, pageableResolver.resolve(page, size))
				.map(lessonMapper::toResponse);
	}
}
