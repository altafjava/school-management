package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
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
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

	@Mock
	private StudentRepository studentRepository;

	private StudentService studentService;

	@BeforeEach
	void setUp() {
		studentService = new StudentService(studentRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void withdraw_transitionsEnrollmentStatusToWithdrawn_withoutSoftDeleting() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-001", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		studentService.withdraw(publicId.toString());

		ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
		verify(studentRepository).save(captor.capture());
		assertEquals(EnrollmentStatus.WITHDRAWN, captor.getValue().getEnrollmentStatus());
		assertFalse(captor.getValue().isDeleted(),
				"withdraw() must transition enrollmentStatus without soft-deleting — a withdrawn student "
						+ "stays visible to historical/guardian views");
	}

	@Test
	void graduate_transitionsEnrollmentStatusToGraduated_withoutSoftDeleting() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-002", "Bob", "Jones", "bob@school.test", LocalDate.of(2009, 1, 1));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		studentService.graduate(publicId.toString());

		ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
		verify(studentRepository).save(captor.capture());
		assertEquals(EnrollmentStatus.GRADUATED, captor.getValue().getEnrollmentStatus());
		assertFalse(captor.getValue().isDeleted());
	}

	@Test
	void updateContactDetails_replacesMutableFields() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-003", "Carol", "Lee", "carol@school.test", LocalDate.of(2010, 5, 5));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		Student updated = studentService.updateContactDetails(publicId.toString(), "Carolyn", "Jones",
				"carolyn@school.test", LocalDate.of(2010, 6, 6));

		assertEquals("Carolyn", updated.getFirstName());
		assertEquals("Jones", updated.getLastName());
		assertEquals("carolyn@school.test", updated.getEmail());
	}

	@Test
	void listStudents_withStatusFilter_delegatesToStatusFilteredQuery() {
		when(studentRepository.findAllByTenantIdAndEnrollmentStatus(1L, EnrollmentStatus.ACTIVE,
				org.springframework.data.domain.PageRequest.of(0, 20)))
				.thenReturn(org.springframework.data.domain.Page.empty());

		studentService.listStudents(org.springframework.data.domain.PageRequest.of(0, 20), EnrollmentStatus.ACTIVE);

		verify(studentRepository).findAllByTenantIdAndEnrollmentStatus(1L, EnrollmentStatus.ACTIVE,
				org.springframework.data.domain.PageRequest.of(0, 20));
	}
}
