package com.altafjava.school.application.privacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentGuardianPiiHandlerTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private GuardianRepository guardianRepository;

	private StudentGuardianPiiHandler piiHandler;

	private void newHandler() {
		piiHandler = new StudentGuardianPiiHandler(studentRepository, guardianRepository);
	}

	@Test
	void erase_withLinkedStudentAndGuardian_erasesAndSoftDeletesBoth() {
		newHandler();
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		Guardian guardian = Guardian.create("Jane", "Doe", "jane@school.test", "+14155552671", 42L);
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.of(guardian));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
		when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));

		piiHandler.erase(9L, 42L);

		assertEquals("[erased]", student.getFirstName());
		assertTrue(student.isDeleted());
		assertEquals("[erased]", guardian.getFirstName());
		assertTrue(guardian.isDeleted());
		verify(studentRepository).save(student);
		verify(guardianRepository).save(guardian);
	}

	@Test
	void erase_withNeitherLinked_doesNothing() {
		newHandler();
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());

		piiHandler.erase(9L, 42L);

		verify(studentRepository, never()).save(any());
		verify(guardianRepository, never()).save(any());
	}

	@Test
	void export_withLinkedStudentOnly_returnsOnlyStudentKey() {
		newHandler();
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setPublicId(java.util.UUID.randomUUID());
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());

		Map<String, Object> result = piiHandler.export(9L, 42L);

		assertTrue(result.containsKey("student"));
		assertFalse(result.containsKey("guardian"));
		@SuppressWarnings("unchecked")
		Map<String, Object> studentExport = (Map<String, Object>) result.get("student");
		assertEquals("STU-1", studentExport.get("studentCode"));
		assertEquals("Alice", studentExport.get("firstName"));
	}

	@Test
	void export_withNeitherLinked_returnsEmptyMap() {
		newHandler();
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());

		Map<String, Object> result = piiHandler.export(9L, 42L);

		assertTrue(result.isEmpty());
	}
}
