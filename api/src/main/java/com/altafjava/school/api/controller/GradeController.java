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
import com.altafjava.school.api.dto.request.CorrectGradeRequest;
import com.altafjava.school.api.dto.request.RecordGradeRequest;
import com.altafjava.school.api.dto.response.GradeCorrectionResponse;
import com.altafjava.school.api.dto.response.GradeResponse;
import com.altafjava.school.api.mapper.GradeCorrectionMapper;
import com.altafjava.school.api.mapper.GradeMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.GradeService;

@RestController
@RequestMapping("/api/v1/grades")
public class GradeController {

	private final GradeService gradeService;
	private final GradeMapper gradeMapper;
	private final GradeCorrectionMapper gradeCorrectionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public GradeController(GradeService gradeService, GradeMapper gradeMapper,
			GradeCorrectionMapper gradeCorrectionMapper, SpringDataPageableResolver pageableResolver) {
		this.gradeService = gradeService;
		this.gradeMapper = gradeMapper;
		this.gradeCorrectionMapper = gradeCorrectionMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_READ')")
	public Page<GradeResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return gradeService.listGrades(pageableResolver.resolve(page, size))
				.map(gradeMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_READ')")
	public GradeResponse get(@PathVariable String publicId) {
		return gradeMapper.toResponse(gradeService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_WRITE')")
	public GradeResponse record(@Valid @RequestBody RecordGradeRequest request) {
		return gradeMapper.toResponse(gradeService.record(
				request.studentId(),
				request.examId(),
				request.marks(),
				request.gradedBy()));
	}

	@PatchMapping("/{publicId}/marks")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_WRITE')")
	public GradeResponse correct(@PathVariable String publicId, @Valid @RequestBody CorrectGradeRequest request) {
		return gradeMapper.toResponse(gradeService.correct(publicId, request.marks()));
	}

	@GetMapping("/{publicId}/corrections")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_READ')")
	public Page<GradeCorrectionResponse> listCorrections(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return gradeService.listCorrections(publicId, pageableResolver.resolve(page, size))
				.map(gradeCorrectionMapper::toResponse);
	}
}
