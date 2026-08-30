package com.altafjava.school.application.privacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class SchoolDataRetentionHandlerTest {

	@Mock
	private StudentRepository studentRepository;

	private SchoolDataRetentionHandler handler;

	private Student withdrawnStudent(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		student.withdraw();
		return student;
	}

	@Test
	void enforceRetention_entityTypeNotStudent_returnsZeroWithoutQuerying() {
		handler = new SchoolDataRetentionHandler(studentRepository);

		int affected = handler.enforceRetention(1L, "GUARDIAN", Instant.now(), "ANONYMIZE");

		assertEquals(0, affected);
		verify(studentRepository, never())
				.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(any(), any(), any());
	}

	@Test
	void enforceRetention_anonymizePolicy_erasesPiiAndSoftDeletesEachEligibleStudent() {
		handler = new SchoolDataRetentionHandler(studentRepository);
		Student student = withdrawnStudent(10L);
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				any(), eq(cutoff))).thenReturn(List.of(student));

		int affected = handler.enforceRetention(1L, "STUDENT", cutoff, "ANONYMIZE");

		assertEquals(1, affected);
		assertEquals("[erased]", student.getFirstName());
		assertEquals(true, student.isDeleted());
		verify(studentRepository, times(1)).save(student);
	}

	@Test
	void enforceRetention_softDeletePolicy_softDeletesWithoutErasingPii() {
		handler = new SchoolDataRetentionHandler(studentRepository);
		Student student = withdrawnStudent(11L);
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				any(), eq(cutoff))).thenReturn(List.of(student));

		handler.enforceRetention(1L, "STUDENT", cutoff, "SOFT_DELETE");

		assertEquals("Alice", student.getFirstName());
		assertEquals(true, student.isDeleted());
	}

	@Test
	void enforceRetention_hardDeletePolicy_throwsUnsupportedOperation() {
		handler = new SchoolDataRetentionHandler(studentRepository);
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				any(), eq(cutoff))).thenReturn(List.of(withdrawnStudent(12L)));

		assertThrows(UnsupportedOperationException.class,
				() -> handler.enforceRetention(1L, "STUDENT", cutoff, "HARD_DELETE"));
	}

	@Test
	void enforceRetention_queriesOnlyInactiveEnrollmentStatuses() {
		handler = new SchoolDataRetentionHandler(studentRepository);
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				eq(List.of(EnrollmentStatus.WITHDRAWN, EnrollmentStatus.GRADUATED, EnrollmentStatus.TRANSFERRED)),
				eq(cutoff))).thenReturn(List.of());

		handler.enforceRetention(1L, "STUDENT", cutoff, "ANONYMIZE");

		verify(studentRepository).findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(1L,
				List.of(EnrollmentStatus.WITHDRAWN, EnrollmentStatus.GRADUATED, EnrollmentStatus.TRANSFERRED), cutoff);
	}
}
