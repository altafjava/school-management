package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

	public AdmissionService(AdmissionRepository admissionRepository,
			AdmissionDecisionRepository admissionDecisionRepository,
			AdmissionEnrollmentSaga admissionEnrollmentSaga) {
		this.admissionRepository = admissionRepository;
		this.admissionDecisionRepository = admissionDecisionRepository;
		this.admissionEnrollmentSaga = admissionEnrollmentSaga;
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
	 * Records the decision and, if approved, synchronously runs the enrollment saga (student
	 * creation, guardian linking, notification) — see {@link AdmissionEnrollmentSaga}.
	 * {@code studentCode} is required only for approval; it becomes the new Student's code.
	 *
	 * <p>
	 * Deliberately not {@code @Transactional}: each write below (the decision record, the
	 * approval status flip, then every step inside the saga) commits on its own via the
	 * repository/service call it goes through, rather than being held open in one long-lived
	 * transaction. The saga's compensation logic runs in its own {@code REQUIRES_NEW}
	 * transaction and can only see writes that have already committed — wrapping this whole
	 * method in one transaction would make an earlier step's work invisible to compensation for
	 * a later step's failure, defeating the point of having compensable steps at all.
	 */
	public Admission decide(String publicId, DecisionOutcome outcome, String decidedBy, String notes,
			String studentCode) {
		Admission admission = findByPublicId(publicId);
		if (admission.getStatus() != AdmissionStatus.SUBMITTED
				&& admission.getStatus() != AdmissionStatus.UNDER_REVIEW) {
			throw new BusinessException(
					"Admission " + publicId + " already has a final decision, status=" + admission.getStatus());
		}
		if (outcome == DecisionOutcome.APPROVED && (studentCode == null || studentCode.isBlank())) {
			throw new BusinessException("studentCode is required when approving an admission");
		}

		AdmissionDecision decision = AdmissionDecision.record(admission.getId(), outcome, decidedBy, notes);
		admissionDecisionRepository.save(decision);

		if (outcome == DecisionOutcome.APPROVED) {
			admission.approve();
			admissionRepository.save(admission);
			admissionEnrollmentSaga.enroll(admission.getId(), studentCode);
			return findByPublicId(publicId);
		}

		admission.reject();
		return admissionRepository.save(admission);
	}
}
