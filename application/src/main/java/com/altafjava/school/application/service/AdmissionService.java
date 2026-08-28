package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.altafjava.platform.application.service.EmailService;
import com.altafjava.platform.application.service.approval.RequiresApproval;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.saga.AdmissionEnrollmentSaga;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionDecision;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.admission.model.DecisionOutcome;
import com.altafjava.school.domain.admission.repository.AdmissionDecisionRepository;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;

@Service
public class AdmissionService {

	private final AdmissionRepository admissionRepository;
	private final AdmissionDecisionRepository admissionDecisionRepository;
	private final AdmissionEnrollmentSaga admissionEnrollmentSaga;
	private final EmailService emailService;

	public AdmissionService(AdmissionRepository admissionRepository,
			AdmissionDecisionRepository admissionDecisionRepository,
			AdmissionEnrollmentSaga admissionEnrollmentSaga, EmailService emailService) {
		this.admissionRepository = admissionRepository;
		this.admissionDecisionRepository = admissionDecisionRepository;
		this.admissionEnrollmentSaga = admissionEnrollmentSaga;
		this.emailService = emailService;
	}

	@Transactional(readOnly = true)
	public Page<Admission> listAdmissions(Pageable pageable) {
		return admissionRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Admission findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return admissionRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Admission not found: " + publicId));
	}

	@Transactional
	public Admission submit(String applicantFirstName, String applicantLastName, LocalDate applicantDateOfBirth,
			String guardianFirstName, String guardianLastName, String guardianEmail, String guardianPhone,
			String appliedGrade) {
		Admission admission = Admission.submit(applicantFirstName, applicantLastName, applicantDateOfBirth,
				guardianFirstName, guardianLastName, guardianEmail, guardianPhone, appliedGrade);
		return admissionRepository.save(admission);
	}

	@Transactional
	public Admission markUnderReview(String publicId) {
		Admission admission = findByPublicId(publicId);
		if (admission.getStatus() != AdmissionStatus.SUBMITTED) {
			throw new BusinessException(
					"Admission " + publicId + " must be SUBMITTED to move under review, was " + admission.getStatus());
		}
		admission.markUnderReview();
		return admissionRepository.save(admission);
	}

	/**
	 * Rejecting an application takes effect immediately — unlike approval, it doesn't create new
	 * platform state (no student enrolled) and doesn't need the same sign-off, so it stays outside
	 * the approval workflow engine.
	 */
	@Transactional
	public Admission reject(String publicId, String decidedBy, String notes) {
		Admission admission = findByPublicId(publicId);
		requireDecidable(admission, publicId);

		AdmissionDecision decision = AdmissionDecision.record(admission.getId(), DecisionOutcome.REJECTED, decidedBy,
				notes);
		admissionDecisionRepository.save(decision);

		admission.reject();
		Admission rejected = admissionRepository.save(admission);
		notifyGuardian(rejected, "Admission Decision",
				"The admission application for " + rejected.getApplicantFirstName() + " "
						+ rejected.getApplicantLastName() + " was not approved at this time.");
		return rejected;
	}

	/**
	 * Submits an approval to enroll {@code studentCode} for this admission — gated by the
	 * tenant's {@code ADMISSION_DECISION} workflow (see {@code AdmissionApprovalHandler}, the
	 * deferred action that runs once every configured stage approves). This method's own body
	 * only ever runs when a tenant genuinely has no active workflow for that operation
	 * ({@code ApprovalAspect} intercepts the call and throws {@code ApprovalPendingException}
	 * instead whenever one exists) — it finalizes immediately in that fallback case, identically
	 * to what the handler does once approved.
	 * <p>
	 * Deliberately not {@code @Transactional} in the finalize path: each write (the decision
	 * record, the approval status flip, then every step inside the saga) commits on its own via
	 * the repository/service call it goes through, rather than being held open in one long-lived
	 * transaction — see {@link AdmissionEnrollmentSaga}'s own compensation rationale.
	 */
	@RequiresApproval(operationCode = "ADMISSION_DECISION", entityIdExpression = "#publicId", payloadExpression = "{'studentCode': #studentCode, 'decidedBy': #decidedBy, 'notes': #notes}")
	public Admission requestApproval(String publicId, String decidedBy, String notes, String studentCode) {
		if (studentCode == null || studentCode.isBlank()) {
			throw new BusinessException("studentCode is required when approving an admission");
		}
		requireDecidable(findByPublicId(publicId), publicId);
		return finalizeApproval(publicId, decidedBy, notes, studentCode);
	}

	/**
	 * Called both by {@link #requestApproval}'s no-workflow fallback and by
	 * {@code AdmissionApprovalHandler} once the configured workflow's final stage approves.
	 */
	public Admission finalizeApproval(String publicId, String decidedBy, String notes, String studentCode) {
		Admission admission = findByPublicId(publicId);
		requireDecidable(admission, publicId);

		AdmissionDecision decision = AdmissionDecision.record(admission.getId(), DecisionOutcome.APPROVED, decidedBy,
				notes);
		admissionDecisionRepository.save(decision);

		admission.approve();
		admissionRepository.save(admission);
		notifyGuardian(admission, "Admission Approved",
				"Congratulations — the admission application for " + admission.getApplicantFirstName() + " "
						+ admission.getApplicantLastName() + " has been approved.");
		admissionEnrollmentSaga.enroll(admission.getId(), studentCode);
		return findByPublicId(publicId);
	}

	private void requireDecidable(Admission admission, String publicId) {
		if (admission.getStatus() != AdmissionStatus.SUBMITTED
				&& admission.getStatus() != AdmissionStatus.UNDER_REVIEW) {
			throw new BusinessException(
					"Admission " + publicId + " already has a final decision, status=" + admission.getStatus());
		}
	}

	@Transactional
	public Admission recordEntranceTestScore(String publicId, BigDecimal score, BigDecimal maxScore) {
		Admission admission = findByPublicId(publicId);
		admission.recordEntranceTestScore(score, maxScore);
		return admissionRepository.save(admission);
	}

	/**
	 * Ranks every scored, still-{@code UNDER_REVIEW} applicant for {@code appliedGrade} by
	 * descending entrance-test score, assigning {@code meritRank} 1..N to all of them. The top
	 * {@code availableSeats} stay {@code UNDER_REVIEW} (ready for the normal {@link #decide}
	 * flow); the rest transition to {@code WAITLISTED}.
	 */
	@Transactional
	public List<Admission> generateMeritList(String appliedGrade, int availableSeats) {
		Long tenantId = TenantContext.getCurrentTenantId();
		List<Admission> candidates = admissionRepository
				.findAllByTenantIdAndAppliedGradeAndStatusAndEntranceTestScoreIsNotNull(tenantId, appliedGrade,
						AdmissionStatus.UNDER_REVIEW);
		candidates.sort(Comparator.comparing(Admission::getEntranceTestScore).reversed());

		List<Admission> ranked = new ArrayList<>(candidates.size());
		for (int i = 0; i < candidates.size(); i++) {
			Admission admission = candidates.get(i);
			int rank = i + 1;
			admission.assignMeritRank(rank);
			boolean seatAvailable = rank <= availableSeats;
			if (!seatAvailable) {
				admission.waitlist();
			}
			notifyMeritListOutcome(admission, appliedGrade, rank, seatAvailable);
			ranked.add(admissionRepository.save(admission));
		}

		return ranked;
	}

	private void notifyMeritListOutcome(Admission admission, String appliedGrade, int rank, boolean seatAvailable) {
		String outcome = seatAvailable
				? " has been ranked #" + rank + " and remains under review for admission."
				: " has been placed on the waitlist at rank " + rank + ".";
		notifyGuardian(admission, "Merit List Published",
				"The merit list for " + appliedGrade + " has been published. " + admission.getApplicantFirstName()
						+ " " + admission.getApplicantLastName() + outcome);
	}

	@Transactional
	public Admission promoteFromWaitlist(String publicId) {
		Admission admission = findByPublicId(publicId);
		admission.promoteFromWaitlist();
		Admission promoted = admissionRepository.save(admission);
		notifyGuardian(promoted, "Admission Update",
				"Good news — " + promoted.getApplicantFirstName() + " " + promoted.getApplicantLastName()
						+ " has been moved from the waitlist back under review.");
		return promoted;
	}

	// Pre-enrollment admissions have no platform userId yet, only a guardianEmail string, so this
	// goes through EmailService directly rather than the in-app NotificationService (which requires
	// a userId). guardianEmail is optional at intake, so a missing address is a silent no-op, not
	// an error.
	private void notifyGuardian(Admission admission, String subject, String message) {
		if (!StringUtils.hasText(admission.getGuardianEmail())) {
			return;
		}
		emailService.sendEmail(admission.getGuardianEmail(), subject, "<p>" + message + "</p>");
	}
}
