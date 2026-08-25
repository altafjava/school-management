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
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.CreatePeriodRequest;
import com.altafjava.school.api.dto.response.PeriodResponse;
import com.altafjava.school.api.mapper.PeriodMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.PeriodService;

@RestController
@RequestMapping("/api/v1/periods")
public class PeriodController {

	private final PeriodService periodService;
	private final PeriodMapper periodMapper;

	private final SpringDataPageableResolver pageableResolver;

	public PeriodController(PeriodService periodService, PeriodMapper periodMapper,
			SpringDataPageableResolver pageableResolver) {
		this.periodService = periodService;
		this.periodMapper = periodMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<PeriodResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return periodService.listPeriods(pageableResolver.resolve(page, size))
				.map(periodMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public PeriodResponse get(@PathVariable String publicId) {
		return periodMapper.toResponse(periodService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public PeriodResponse create(@Valid @RequestBody CreatePeriodRequest request) {
		return periodMapper.toResponse(periodService.create(
				request.name(),
				request.startTime(),
				request.endTime(),
				request.displayOrder()));
	}
}
