package com.altafjava.school.api.controller;

import java.util.List;
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
import com.altafjava.school.api.dto.request.CreateGradingScaleRequest;
import com.altafjava.school.api.dto.request.UpdateGradingScaleThresholdsRequest;
import com.altafjava.school.api.dto.response.GradingScaleResponse;
import com.altafjava.school.api.mapper.GradingScaleMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.application.service.GradingScaleThresholdInput;
import com.altafjava.school.domain.curriculum.model.GradingScale;

@RestController
@RequestMapping("/api/v1/grading-scales")
public class GradingScaleController {

	private final GradingScaleService gradingScaleService;
	private final GradingScaleMapper gradingScaleMapper;

	private final SpringDataPageableResolver pageableResolver;

	public GradingScaleController(GradingScaleService gradingScaleService, GradingScaleMapper gradingScaleMapper,
			SpringDataPageableResolver pageableResolver) {
		this.gradingScaleService = gradingScaleService;
		this.gradingScaleMapper = gradingScaleMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<GradingScaleResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return gradingScaleService.list(pageableResolver.resolve(page, size)).map(this::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public GradingScaleResponse get(@PathVariable String publicId) {
		return toResponse(gradingScaleService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GradingScaleResponse create(@Valid @RequestBody CreateGradingScaleRequest request) {
		GradingScale scale = gradingScaleService.create(request.name(), toInputs(request), request.isDefault());
		return toResponse(scale);
	}

	@PatchMapping("/{publicId}/thresholds")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GradingScaleResponse updateThresholds(@PathVariable String publicId,
			@Valid @RequestBody UpdateGradingScaleThresholdsRequest request) {
		var thresholds = request.thresholds().stream()
				.map(t -> new GradingScaleThresholdInput(t.letter(), t.minPercentage(), t.points()))
				.toList();
		return toResponse(gradingScaleService.updateThresholds(publicId, thresholds));
	}

	@PatchMapping("/{publicId}/default")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GradingScaleResponse markAsDefault(@PathVariable String publicId) {
		return toResponse(gradingScaleService.markAsDefault(publicId));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public GradingScaleResponse deactivate(@PathVariable String publicId) {
		return toResponse(gradingScaleService.deactivate(publicId));
	}

	private List<GradingScaleThresholdInput> toInputs(CreateGradingScaleRequest request) {
		return request.thresholds().stream()
				.map(t -> new GradingScaleThresholdInput(t.letter(), t.minPercentage(), t.points()))
				.toList();
	}

	private GradingScaleResponse toResponse(GradingScale scale) {
		var thresholds = gradingScaleService.listThresholds(scale.getPublicId().toString()).stream()
				.map(gradingScaleMapper::toThreshold)
				.toList();
		return new GradingScaleResponse(scale.getPublicId().toString(), scale.getName(), scale.isDefault(),
				scale.isActive(), thresholds);
	}
}
