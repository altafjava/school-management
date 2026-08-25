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
import com.altafjava.school.api.dto.request.CreateAlumniProfileRequest;
import com.altafjava.school.api.dto.request.UpdateAlumniContactInfoRequest;
import com.altafjava.school.api.dto.response.AlumniProfileResponse;
import com.altafjava.school.api.mapper.AlumniProfileMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AlumniProfileService;

/**
 * Gated to {@code Roles.HAS_TENANT_ADMIN} on every endpoint — alumni directory data carries
 * {@code @Pii} contact details, and this is staff-managed directory data, not an alumni self-service
 * portal (out of scope per the plan's basic-tracking scope). Event participation for an alumnus goes
 * through the existing {@code EventRegistrationController} using their original student public id
 * (see {@code AlumniProfile}'s javadoc) — no separate registration endpoint exists here.
 */
@RestController
@RequestMapping("/api/v1/alumni-profiles")
public class AlumniProfileController {

	private final AlumniProfileService alumniProfileService;
	private final AlumniProfileMapper alumniProfileMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AlumniProfileController(AlumniProfileService alumniProfileService, AlumniProfileMapper alumniProfileMapper,
			SpringDataPageableResolver pageableResolver) {
		this.alumniProfileService = alumniProfileService;
		this.alumniProfileMapper = alumniProfileMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<AlumniProfileResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return alumniProfileService.list(pageableResolver.resolve(page, size)).map(alumniProfileMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AlumniProfileResponse get(@PathVariable String publicId) {
		return alumniProfileMapper.toResponse(alumniProfileService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AlumniProfileResponse create(@Valid @RequestBody CreateAlumniProfileRequest request) {
		return alumniProfileMapper.toResponse(alumniProfileService.create(request.studentPublicId(),
				request.graduationYear(), request.currentOccupation(), request.contactEmail(),
				request.contactPhone()));
	}

	@PatchMapping("/{publicId}/contact-info")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AlumniProfileResponse updateContactInfo(@PathVariable String publicId,
			@Valid @RequestBody UpdateAlumniContactInfoRequest request) {
		return alumniProfileMapper.toResponse(alumniProfileService.updateContactInfo(publicId,
				request.currentOccupation(), request.contactEmail(), request.contactPhone()));
	}

	@PatchMapping("/{publicId}/activate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AlumniProfileResponse activate(@PathVariable String publicId) {
		return alumniProfileMapper.toResponse(alumniProfileService.activate(publicId));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AlumniProfileResponse deactivate(@PathVariable String publicId) {
		return alumniProfileMapper.toResponse(alumniProfileService.deactivate(publicId));
	}
}
