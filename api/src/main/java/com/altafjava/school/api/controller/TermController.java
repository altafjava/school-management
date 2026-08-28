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
import com.altafjava.school.api.dto.request.CreateTermRequest;
import com.altafjava.school.api.dto.response.TermResponse;
import com.altafjava.school.api.mapper.TermMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.TermService;

@RestController
@RequestMapping("/api/v1/terms")
public class TermController {

	private final TermService termService;
	private final TermMapper termMapper;

	private final SpringDataPageableResolver pageableResolver;

	public TermController(TermService termService, TermMapper termMapper, SpringDataPageableResolver pageableResolver) {
		this.termService = termService;
		this.termMapper = termMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TERM_READ')")
	public Page<TermResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return termService.listTerms(pageableResolver.resolve(page, size))
				.map(termMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TERM_READ')")
	public TermResponse get(@PathVariable String publicId) {
		return termMapper.toResponse(termService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TERM_WRITE')")
	public TermResponse create(@Valid @RequestBody CreateTermRequest request) {
		return termMapper.toResponse(termService.create(
				request.name(),
				request.startDate(),
				request.endDate(),
				request.academicYearId()));
	}
}
