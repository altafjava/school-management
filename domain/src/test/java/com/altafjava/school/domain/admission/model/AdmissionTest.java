package com.altafjava.school.domain.admission.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdmissionTest {

	private Admission submitted() {
		return Admission.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith", "bob@family.test",
				"555-1234", "Grade 3");
	}

	@Test
	void submit_setsFieldsAndDefaultsToSubmittedStatus() {
		Admission admission = submitted();

		assertEquals("Alice", admission.getApplicantFirstName());
		assertEquals("Smith", admission.getApplicantLastName());
		assertEquals("Bob", admission.getGuardianFirstName());
		assertEquals("Grade 3", admission.getAppliedGrade());
		assertEquals(AdmissionStatus.SUBMITTED, admission.getStatus());
	}

	@Test
	void markUnderReview_transitionsStatus() {
		Admission admission = submitted();

		admission.markUnderReview();

		assertEquals(AdmissionStatus.UNDER_REVIEW, admission.getStatus());
	}

	@Test
	void approve_transitionsStatus() {
		Admission admission = submitted();

		admission.approve();

		assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
	}

	@Test
	void reject_transitionsStatus() {
		Admission admission = submitted();

		admission.reject();

		assertEquals(AdmissionStatus.REJECTED, admission.getStatus());
	}

	@Test
	void enrollmentLifecycle_tracksSagaAndCreatedRecords() {
		Admission admission = submitted();
		UUID sagaId = UUID.randomUUID();

		admission.beginEnrollmentSaga(sagaId);
		admission.recordEnrolledStudent(10L);
		admission.recordEnrolledGuardian(20L);
		admission.markEnrolled();

		assertEquals(sagaId, admission.getEnrollmentSagaId());
		assertEquals(10L, admission.getEnrolledStudentId());
		assertEquals(20L, admission.getEnrolledGuardianId());
		assertEquals(AdmissionStatus.ENROLLED, admission.getStatus());
	}

	@Test
	void revertEnrollment_clearsTrackingFieldsAndRestoresApprovedStatus() {
		Admission admission = submitted();
		admission.beginEnrollmentSaga(UUID.randomUUID());
		admission.recordEnrolledStudent(10L);
		admission.recordEnrolledGuardian(20L);
		admission.markEnrolled();

		admission.revertEnrollment();

		assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
		assertNull(admission.getEnrolledStudentId());
		assertNull(admission.getEnrolledGuardianId());
		assertNull(admission.getEnrollmentSagaId());
	}
}
