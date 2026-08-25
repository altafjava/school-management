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
import com.altafjava.school.api.dto.request.DecideAdmissionRequest;
import com.altafjava.school.api.dto.request.PublicAdmissionApplicationRequest;
import com.altafjava.school.api.dto.request.RecordEntranceTestScoreRequest;
import com.altafjava.school.api.dto.request.SubmitAdmissionRequest;
import com.altafjava.school.api.dto.response.AdmissionResponse;
import com.altafjava.school.api.mapper.AdmissionMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AdmissionService;

/**
 * All endpoints here require {@code TENANT_ADMIN} except {@link #apply}, which is intended to be
 * unauthenticated (a prospective guardian applying before any account exists) — see that method's
 * Javadoc for the current, incomplete state of that intent.
 * <p>
 * {@code @PreAuthorize} is declared per method rather than at the class level (matching
 * {@code AttendanceController}/{@code ExamController}/{@code FeeStructureController}/
 * {@code GuardianController}'s convention) specifically so {@link #apply} can be exempted —
 * Spring Security method security does not let a method-level annotation override/remove a
 * class-level one on the same class.
 */
@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionController {

	private final AdmissionService admissionService;
	private final AdmissionMapper admissionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AdmissionController(AdmissionService admissionService, AdmissionMapper admissionMapper,
			SpringDataPageableResolver pageableResolver) {
		this.admissionService = admissionService;
		this.admissionMapper = admissionMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<AdmissionResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return admissionService.listAdmissions(pageableResolver.resolve(page, size))
				.map(admissionMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AdmissionResponse get(@PathVariable String publicId) {
		return admissionMapper.toResponse(admissionService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AdmissionResponse submit(@Valid @RequestBody SubmitAdmissionRequest request) {
		return admissionMapper.toResponse(admissionService.submit(
				request.applicantFirstName(),
				request.applicantLastName(),
				request.applicantDateOfBirth(),
				request.guardianFirstName(),
				request.guardianLastName(),
				request.guardianEmail(),
				request.guardianPhone(),
				request.appliedGrade()));
	}

	/**
	 * Public, unauthenticated intake for a prospective guardian applying before any account
	 * exists — no {@code @PreAuthorize}, mirroring platform's {@code AuthController.register()}.
	 * Tenant is resolved the normal way (subdomain/header), never accepted from the request body.
	 * Reachable without a JWT via a literal {@code /api/v1/admissions/apply} entry in platform-saas's
	 * {@code SecurityConfig} permitAll allowlist.
	 */
	@PostMapping("/apply")
	@ResponseStatus(HttpStatus.CREATED)
	public AdmissionResponse apply(@Valid @RequestBody PublicAdmissionApplicationRequest request) {
		return admissionMapper.toResponse(admissionService.submit(
				request.applicantFirstName(),
				request.applicantLastName(),
				request.applicantDateOfBirth(),
				request.guardianFirstName(),
				request.guardianLastName(),
				request.guardianEmail(),
				request.guardianPhone(),
				request.appliedGrade()));
	}

	@PatchMapping("/{publicId}/under-review")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AdmissionResponse markUnderReview(@PathVariable String publicId) {
		return admissionMapper.toResponse(admissionService.markUnderReview(publicId));
	}

	@PatchMapping("/{publicId}/decision")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AdmissionResponse decide(@PathVariable String publicId, @Valid @RequestBody DecideAdmissionRequest request) {
		return admissionMapper.toResponse(admissionService.decide(
				publicId,
				request.outcome(),
				request.decidedBy(),
				request.notes(),
				request.studentCode()));
	}

	@PostMapping("/{publicId}/entrance-test-score")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AdmissionResponse recordEntranceTestScore(@PathVariable String publicId,
			@Valid @RequestBody RecordEntranceTestScoreRequest request) {
		return admissionMapper.toResponse(
				admissionService.recordEntranceTestScore(publicId, request.score(), request.maxScore()));
	}

	@PostMapping("/merit-list")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public List<AdmissionResponse> generateMeritList(
			@RequestParam String appliedGrade,
			@RequestParam int availableSeats) {
		return admissionService.generateMeritList(appliedGrade, availableSeats).stream()
				.map(admissionMapper::toResponse)
				.toList();
	}

	@PatchMapping("/{publicId}/promote-from-waitlist")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public AdmissionResponse promoteFromWaitlist(@PathVariable String publicId) {
		return admissionMapper.toResponse(admissionService.promoteFromWaitlist(publicId));
	}
}
