package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.DecideAdmissionRequest;
import com.altafjava.school.api.dto.request.PublicAdmissionApplicationRequest;
import com.altafjava.school.api.dto.request.RecordEntranceTestScoreRequest;
import com.altafjava.school.api.dto.request.SubmitAdmissionRequest;
import com.altafjava.school.api.dto.response.AdmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admission", description = "APIs for managing Admission operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AdmissionApi {

	@Operation(summary = "List", operationId = "admission_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<AdmissionResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "admission_get")
	public ApiResponse<AdmissionResponse> get(@PathVariable String publicId);

	@Operation(summary = "Submit", operationId = "admission_submit")
	public ApiResponse<AdmissionResponse> submit(@Valid @RequestBody SubmitAdmissionRequest request);

	/**
	 * Public, unauthenticated intake for a prospective guardian applying before any account
	 * exists — no {@code @PreAuthorize}, mirroring platform's {@code AuthController.register()}.
	 * Tenant is resolved the normal way (subdomain/header), never accepted from the request body.
	 * Reachable without a JWT via a literal {@code /api/v1/admissions/apply} entry in platform-saas's
	 * {@code SecurityConfig} permitAll allowlist.
	 */
	@Operation(summary = "Apply", operationId = "admission_apply")
	public ApiResponse<AdmissionResponse> apply(@Valid @RequestBody PublicAdmissionApplicationRequest request);

	@Operation(summary = "Mark under review", operationId = "admission_markUnderReview")
	public ApiResponse<AdmissionResponse> markUnderReview(@PathVariable String publicId);

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
	@Operation(summary = "Decide", operationId = "admission_decide")
	public ApiResponse<AdmissionResponse> decide(@PathVariable String publicId,
			@Valid @RequestBody DecideAdmissionRequest request);

	@Operation(summary = "Record entrance test score", operationId = "admission_recordEntranceTestScore")
	public ApiResponse<AdmissionResponse> recordEntranceTestScore(@PathVariable String publicId,
			@Valid @RequestBody RecordEntranceTestScoreRequest request);

	@Operation(summary = "Generate merit list", operationId = "admission_generateMeritList")
	public ApiResponse<List<AdmissionResponse>> generateMeritList(
			@RequestParam String appliedGrade,
			@RequestParam int availableSeats);

	@Operation(summary = "Promote from waitlist", operationId = "admission_promoteFromWaitlist")
	public ApiResponse<AdmissionResponse> promoteFromWaitlist(@PathVariable String publicId);
}
