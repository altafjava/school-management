package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.model.ExamType;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

	@Mock
	private ExamRepository examRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private TermRepository termRepository;

	private ExamService examService;

	@BeforeEach
	void setUp() {
		examService = new ExamService(examRepository, classroomRepository, subjectRepository, termRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void schedule_withNonExistentClassroom_throwsResourceNotFound() {
		when(classroomRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> examService.schedule("Midterm", 5L, 99L, LocalDateTime.now().plusDays(7),
						BigDecimal.valueOf(100), null, ExamType.MIDTERM));

		verify(examRepository, never()).save(any());
	}

	@Test
	void schedule_withNonExistentSubject_throwsResourceNotFound() {
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(subjectRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> examService.schedule("Midterm", 99L, 10L, LocalDateTime.now().plusDays(7),
						BigDecimal.valueOf(100), null, ExamType.MIDTERM));

		verify(examRepository, never()).save(any());
	}

	@Test
	void schedule_withExistingClassroomAndSubject_succeeds() {
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(subjectRepository.existsByIdAndTenantId(5L, 1L)).thenReturn(true);
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> examService.schedule("Midterm", 5L, 10L, LocalDateTime.now().plusDays(7),
				BigDecimal.valueOf(100), null, ExamType.MIDTERM));
	}

	@Test
	void schedule_withNonExistentTerm_throwsResourceNotFound() {
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(subjectRepository.existsByIdAndTenantId(5L, 1L)).thenReturn(true);
		when(termRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> examService.schedule("Midterm", 5L, 10L, LocalDateTime.now().plusDays(7),
						BigDecimal.valueOf(100), 99L, ExamType.MIDTERM));

		verify(examRepository, never()).save(any());
	}

	private Exam examWithPublicId(UUID publicId) {
		Exam exam = Exam.create("Midterm", 5L, 10L, LocalDateTime.now().plusDays(7), BigDecimal.valueOf(100), null,
				ExamType.MIDTERM);
		exam.setPublicId(publicId);
		return exam;
	}

	@Test
	void reschedule_updatesScheduledAt() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		LocalDateTime newTime = LocalDateTime.now().plusDays(14);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam rescheduled = examService.reschedule(publicId.toString(), newTime);

		assertEquals(newTime, rescheduled.getScheduledAt());
	}

	@Test
	void assignTerm_withExistingTerm_succeeds() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(termRepository.existsByIdAndTenantId(7L, 1L)).thenReturn(true);
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam updated = examService.assignTerm(publicId.toString(), 7L);

		assertEquals(7L, updated.getTermId());
	}

	@Test
	void assignTerm_withNonExistentTerm_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(termRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> examService.assignTerm(publicId.toString(), 99L));
	}

	@Test
	void complete_scheduledExam_setsStatusCompleted() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam completed = examService.complete(publicId.toString());

		assertEquals(ExamStatus.COMPLETED, completed.getStatus());
	}

	@Test
	void complete_alreadyCancelledExam_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		exam.cancel();
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));

		assertThrows(BusinessException.class, () -> examService.complete(publicId.toString()));
	}

	@Test
	void cancel_scheduledExam_setsStatusCancelled() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam cancelled = examService.cancel(publicId.toString());

		assertEquals(ExamStatus.CANCELLED, cancelled.getStatus());
	}

	@Test
	void cancel_alreadyCompletedExam_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		exam.complete();
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));

		assertThrows(BusinessException.class, () -> examService.cancel(publicId.toString()));
	}
}
