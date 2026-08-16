package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.model.GradingScale;
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
	@Mock
	private GradingScaleService gradingScaleService;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;

	private GradeService gradeService;

	@BeforeEach
	void setUp() {
		gradeService = new GradeService(gradeRepository, studentRepository, examRepository, gradingScaleService,
				studentDataAccessGuard);
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
				() -> gradeService.record(99L, 1L, BigDecimal.valueOf(85), "teacher"));

		verify(gradeRepository, never()).save(any());
	}

	@Test
	void record_withNonExistentExam_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(examRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> gradeService.record(1L, 99L, BigDecimal.valueOf(85), "teacher"));

		verify(gradeRepository, never()).save(any());
	}

	@Test
	void record_duplicateForSameStudentExam_throwsIllegalArgument() {
		Exam exam = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100));
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(examRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(exam));
		when(gradeRepository.existsByStudentIdAndExamIdAndTenantId(1L, 2L, 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> gradeService.record(1L, 2L, BigDecimal.valueOf(85), "teacher"));
	}

	@Test
	void record_withValidReferences_computesLetterGradeFromDefaultScale() {
		Exam exam = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100));
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(examRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(exam));
		when(gradeRepository.existsByStudentIdAndExamIdAndTenantId(1L, 2L, 1L)).thenReturn(false);
		when(gradingScaleService.getScale()).thenReturn(GradingScale.defaultScale());
		when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

		Grade grade = assertDoesNotThrow(
				() -> gradeService.record(1L, 2L, BigDecimal.valueOf(92), "teacher"));

		assertEquals("A", grade.getGradeLetter());
		assertEquals(5L, grade.getSubjectId());
	}

	@Test
	void getStudentGrades_delegatesToAccessGuard() {
		doNothing().when(studentDataAccessGuard).assertCanView(any(), any());
		when(studentRepository.findByPublicIdAndTenantId(any(), any()))
				.thenReturn(Optional.of(com.altafjava.school.domain.student.model.Student.create(
						"STU-1", "Alice", "Smith", "alice@school.test", null)));

		assertDoesNotThrow(() -> gradeService.getStudentGrades(
				"11111111-1111-1111-1111-111111111111", org.springframework.data.domain.PageRequest.of(0, 20)));

		verify(studentDataAccessGuard).assertCanView(1L, "11111111-1111-1111-1111-111111111111");
	}
}
