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
import com.altafjava.school.api.dto.request.ScheduleCounselingSessionRequest;
import com.altafjava.school.api.dto.request.UpdateCounselingSessionNotesRequest;
import com.altafjava.school.api.dto.response.CounselingSessionResponse;
import com.altafjava.school.api.mapper.CounselingSessionMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CounselingSessionService;

/**
 * Gated to {@code Roles.HAS_TENANT_ADMIN} only on every endpoint, including reads — the same
 * confidentiality posture as {@code HealthRecordController}/{@code MedicalIncidentController}, since
 * counseling notes are PHI-grade confidential data. No dedicated counselor/school-psychologist role
 * exists in the seeded role catalog; a dedicated role is a follow-up.
 */
@RestController
@RequestMapping("/api/v1/counseling-sessions")
public class CounselingSessionController {

	private final CounselingSessionService counselingSessionService;
	private final CounselingSessionMapper counselingSessionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CounselingSessionController(CounselingSessionService counselingSessionService,
			CounselingSessionMapper counselingSessionMapper, SpringDataPageableResolver pageableResolver) {
		this.counselingSessionService = counselingSessionService;
		this.counselingSessionMapper = counselingSessionMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<CounselingSessionResponse> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return counselingSessionService.listAll(pageableResolver.resolve(page, size))
				.map(counselingSessionMapper::toResponse);
	}

	@GetMapping("/students/{studentPublicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<CounselingSessionResponse> listForStudent(@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return counselingSessionService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
				.map(counselingSessionMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public CounselingSessionResponse get(@PathVariable String publicId) {
		return counselingSessionMapper.toResponse(counselingSessionService.get(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public CounselingSessionResponse schedule(@Valid @RequestBody ScheduleCounselingSessionRequest request) {
		return counselingSessionMapper.toResponse(counselingSessionService.schedule(request.studentPublicId(),
				request.counselorTeacherPublicId(), request.sessionDate(), request.notes(),
				request.followUpRequired()));
	}

	@PatchMapping("/{publicId}/notes")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public CounselingSessionResponse updateNotes(@PathVariable String publicId,
			@Valid @RequestBody UpdateCounselingSessionNotesRequest request) {
		return counselingSessionMapper.toResponse(
				counselingSessionService.updateNotes(publicId, request.notes(), request.followUpRequired()));
	}
}
