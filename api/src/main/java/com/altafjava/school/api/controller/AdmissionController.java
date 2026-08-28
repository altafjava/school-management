package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.api.dto.request.DecideAdmissionRequest;
import com.altafjava.school.api.dto.request.PublicAdmissionApplicationRequest;
import com.altafjava.school.api.dto.request.RecordEntranceTestScoreRequest;
import com.altafjava.school.api.dto.request.SubmitAdmissionRequest;
import com.altafjava.school.api.dto.response.AdmissionResponse;
import com.altafjava.school.api.mapper.AdmissionMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.domain.admission.model.DecisionOutcome;

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
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public Page<AdmissionResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return admissionService.listAdmissions(pageableResolver.resolve(page, size))
				.map(admissionMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public AdmissionResponse get(@PathVariable String publicId) {
		return admissionMapper.toResponse(admissionService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
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
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public AdmissionResponse markUnderReview(@PathVariable String publicId) {
		return admissionMapper.toResponse(admissionService.markUnderReview(publicId));
	}

	/**
	 * A REJECTED outcome takes effect immediately; an APPROVED outcome submits for the tenant's
	 * {@code ADMISSION_DECISION} approval workflow instead of enrolling directly — see
	 * {@code AdmissionService#requestApproval}. When gated, this returns 202 with the new approval
	 * request's ID (via {@code GlobalExceptionHandler}'s {@code ApprovalPendingException} handling)
	 * rather than the 200 {@link AdmissionResponse} below.
	 * <p>
	 * The studentCode-required-for-approval check happens here, not inside
	 * {@code requestApproval}: once a workflow is configured, {@code ApprovalAspect} intercepts
	 * that call and never runs its body at all, so any validation inside it would be silently
	 * skipped for every gated request.
	 */
	@PatchMapping("/{publicId}/decision")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public AdmissionResponse decide(@PathVariable String publicId, @Valid @RequestBody DecideAdmissionRequest request) {
		if (request.outcome() != DecisionOutcome.APPROVED) {
			return admissionMapper.toResponse(admissionService.reject(publicId, request.decidedBy(), request.notes()));
		}
		if (!StringUtils.hasText(request.studentCode())) {
			throw new BusinessException("studentCode is required when approving an admission");
		}
		return admissionMapper.toResponse(admissionService.requestApproval(publicId, request.decidedBy(),
				request.notes(), request.studentCode()));
	}

	@PostMapping("/{publicId}/entrance-test-score")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public AdmissionResponse recordEntranceTestScore(@PathVariable String publicId,
			@Valid @RequestBody RecordEntranceTestScoreRequest request) {
		return admissionMapper.toResponse(
				admissionService.recordEntranceTestScore(publicId, request.score(), request.maxScore()));
	}

	@PostMapping("/merit-list")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public List<AdmissionResponse> generateMeritList(
			@RequestParam String appliedGrade,
			@RequestParam int availableSeats) {
		return admissionService.generateMeritList(appliedGrade, availableSeats).stream()
				.map(admissionMapper::toResponse)
				.toList();
	}

	@PatchMapping("/{publicId}/promote-from-waitlist")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public AdmissionResponse promoteFromWaitlist(@PathVariable String publicId) {
		return admissionMapper.toResponse(admissionService.promoteFromWaitlist(publicId));
	}
}
