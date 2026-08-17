package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.saga.AdmissionEnrollmentSaga;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionDecision;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.admission.model.DecisionOutcome;
import com.altafjava.school.domain.admission.repository.AdmissionDecisionRepository;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

	@Mock
	private AdmissionRepository admissionRepository;
	@Mock
	private AdmissionDecisionRepository admissionDecisionRepository;
	@Mock
	private AdmissionEnrollmentSaga admissionEnrollmentSaga;

	private AdmissionService admissionService;

	@BeforeEach
	void setUp() {
		admissionService = new AdmissionService(admissionRepository, admissionDecisionRepository,
				admissionEnrollmentSaga);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Admission admissionWithId(long id, UUID publicId, AdmissionStatus status) {
		Admission admission = Admission.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith",
				"bob@family.test", "555-1234", "Grade 3");
		admission.setId(id);
		admission.setPublicId(publicId);
		admission.setStatus(status);
		return admission;
	}

	@Test
	void submit_createsAdmissionWithSubmittedStatus() {
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));

		Admission admission = admissionService.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith",
				"bob@family.test", "555-1234", "Grade 3");

		assertEquals(AdmissionStatus.SUBMITTED, admission.getStatus());
	}

	@Test
	void markUnderReview_fromSubmitted_succeeds() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));

		Admission result = admissionService.markUnderReview(publicId.toString());

		assertEquals(AdmissionStatus.UNDER_REVIEW, result.getStatus());
	}

	@Test
	void markUnderReview_fromApproved_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.APPROVED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));

		assertThrows(BusinessException.class, () -> admissionService.markUnderReview(publicId.toString()));

		verify(admissionRepository, never()).save(any());
	}

	@Test
	void decide_approveWithoutStudentCode_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));

		assertThrows(BusinessException.class, () -> admissionService.decide(publicId.toString(),
				DecisionOutcome.APPROVED, "admin", null, null));

		verify(admissionEnrollmentSaga, never()).enroll(anyLong(), anyString());
	}

	@Test
	void decide_alreadyEnrolled_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.ENROLLED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));

		assertThrows(BusinessException.class, () -> admissionService.decide(publicId.toString(),
				DecisionOutcome.APPROVED, "admin", null, "STU-100"));

		verify(admissionDecisionRepository, never()).save(any());
	}

	@Test
	void decide_approve_recordsDecisionApprovesAndTriggersSaga() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));
		when(admissionDecisionRepository.save(any(AdmissionDecision.class))).thenAnswer(inv -> inv.getArgument(0));

		admissionService.decide(publicId.toString(), DecisionOutcome.APPROVED, "admin", "looks good", "STU-100");

		ArgumentCaptor<AdmissionDecision> decisionCaptor = ArgumentCaptor.forClass(AdmissionDecision.class);
		verify(admissionDecisionRepository).save(decisionCaptor.capture());
		assertEquals(DecisionOutcome.APPROVED, decisionCaptor.getValue().getOutcome());
		assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
		verify(admissionEnrollmentSaga).enroll(eq(1L), eq("STU-100"));
	}

	@Test
	void decide_reject_recordsDecisionAndDoesNotTriggerSaga() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));
		when(admissionDecisionRepository.save(any(AdmissionDecision.class))).thenAnswer(inv -> inv.getArgument(0));

		Admission result = admissionService.decide(publicId.toString(), DecisionOutcome.REJECTED, "admin",
				"not a fit", null);

		assertEquals(AdmissionStatus.REJECTED, result.getStatus());
		verify(admissionEnrollmentSaga, never()).enroll(anyLong(), anyString());
	}
}
