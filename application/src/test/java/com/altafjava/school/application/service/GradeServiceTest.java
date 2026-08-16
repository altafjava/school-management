package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

	@Mock
	private GradeRepository gradeRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private ExamRepository examRepository;

	private GradeService gradeService;

	@BeforeEach
	void setUp() {
		gradeService = new GradeService(gradeRepository, studentRepository, examRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void record_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> gradeService.record(99L, "Math", 1L, BigDecimal.valueOf(85), "A", "teacher"));

		verify(gradeRepository, never()).save(any());
	}

	@Test
	void record_withNonExistentExam_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(examRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> gradeService.record(1L, "Math", 99L, BigDecimal.valueOf(85), "A", "teacher"));

		verify(gradeRepository, never()).save(any());
	}

	@Test
	void record_duplicateForSameStudentExam_throwsIllegalArgument() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(examRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(gradeRepository.existsByStudentIdAndExamIdAndTenantId(1L, 2L, 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> gradeService.record(1L, "Math", 2L, BigDecimal.valueOf(85), "A", "teacher"));
	}

	@Test
	void record_withValidReferences_succeeds() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(examRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(gradeRepository.existsByStudentIdAndExamIdAndTenantId(1L, 2L, 1L)).thenReturn(false);
		when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> gradeService.record(1L, "Math", 2L, BigDecimal.valueOf(85), "A", "teacher"));
	}
}
