package com.altafjava.school.api.controller;

import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.altafjava.school.api.dto.request.CheckInVisitorRequest;
import com.altafjava.school.api.dto.response.VisitorLogResponse;
import com.altafjava.school.api.mapper.VisitorLogMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.VisitorLogService;

/**
 * Gated to {@code SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER} — the closest existing role to a
 * front-desk/receptionist function in the seeded catalog (no dedicated role exists), kept
 * authenticated rather than {@code permitAll()} since visitor logs are internal building-security
 * data, unlike the certificate-verification or admission-application public endpoints. A dedicated
 * front-desk role is a follow-up.
 */
@RestController
@RequestMapping("/api/v1/visitor-logs")
public class VisitorLogController {

	private final VisitorLogService visitorLogService;
	private final VisitorLogMapper visitorLogMapper;

	private final SpringDataPageableResolver pageableResolver;

	public VisitorLogController(VisitorLogService visitorLogService, VisitorLogMapper visitorLogMapper,
			SpringDataPageableResolver pageableResolver) {
		this.visitorLogService = visitorLogService;
		this.visitorLogMapper = visitorLogMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<VisitorLogResponse> list(
			@RequestParam(required = false) Boolean stillCheckedIn,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return visitorLogService.list(stillCheckedIn, from, to, pageableResolver.resolve(page, size))
				.map(visitorLogMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public VisitorLogResponse get(@PathVariable String publicId) {
		return visitorLogMapper.toResponse(visitorLogService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public VisitorLogResponse checkIn(@Valid @RequestBody CheckInVisitorRequest request) {
		return visitorLogMapper.toResponse(visitorLogService.checkIn(request.visitorName(), request.visitorPhone(),
				request.purpose(), request.hostTeacherPublicId()));
	}

	@PatchMapping("/{publicId}/check-out")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public VisitorLogResponse checkOut(@PathVariable String publicId) {
		return visitorLogMapper.toResponse(visitorLogService.checkOut(publicId));
	}
}
