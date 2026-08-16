package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void withdraw_transitionsEnrollmentStatusToWithdrawn_andSoftDeletes() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-001", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		studentService.withdraw(publicId.toString(), "admin@school.test");

		ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
		verify(studentRepository).save(captor.capture());
		assertEquals(EnrollmentStatus.WITHDRAWN, captor.getValue().getEnrollmentStatus(),
				"withdraw() must transition enrollmentStatus, not just soft-delete");
		assertEquals(true, captor.getValue().isDeleted());
	}
}
