package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.UpdateGradingScaleRequest;
import com.altafjava.school.api.dto.response.GradingScaleResponse;
import com.altafjava.school.api.mapper.GradingScaleMapper;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.domain.grade.model.GradeThreshold;

@RestController
@RequestMapping("/api/v1/grading-scale")
public class GradingScaleController {

	private final GradingScaleService gradingScaleService;
	private final GradingScaleMapper gradingScaleMapper;

	public GradingScaleController(GradingScaleService gradingScaleService, GradingScaleMapper gradingScaleMapper) {
		this.gradingScaleService = gradingScaleService;
		this.gradingScaleMapper = gradingScaleMapper;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public GradingScaleResponse get() {
		return gradingScaleMapper.toResponse(gradingScaleService.getScale());
	}

	@PutMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GradingScaleResponse update(@Valid @RequestBody UpdateGradingScaleRequest request) {
		var thresholds = request.thresholds().stream()
				.map(threshold -> new GradeThreshold(threshold.letter(), threshold.minPercentage()))
				.toList();
		return gradingScaleMapper.toResponse(gradingScaleService.updateScale(thresholds));
	}
}
