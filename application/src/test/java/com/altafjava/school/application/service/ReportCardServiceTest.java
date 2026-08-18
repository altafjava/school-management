package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.file.service.StorageService;
import com.altafjava.school.application.reportcard.ReportCardLine;
import com.altafjava.school.application.reportcard.ReportCardPdfGenerator;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.reportcard.model.ReportCard;
import com.altafjava.school.domain.reportcard.repository.ReportCardRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

	@Mock
	private ReportCardRepository reportCardRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TermRepository termRepository;
	@Mock
	private GradeRepository gradeRepository;
	@Mock
	private ExamRepository examRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private StorageService storageService;
	@Mock
	private ReportCardPdfGenerator pdfGenerator;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private EventPublisher eventPublisher;

	private ReportCardService reportCardService;

	@BeforeEach
	void setUp() {
		reportCardService = new ReportCardService(reportCardRepository, studentRepository, termRepository,
				gradeRepository, examRepository, subjectRepository, storageService, pdfGenerator,
				studentDataAccessGuard, eventPublisher);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private Term termWithId(long id, LocalDate start, LocalDate end) {
		Term term = Term.create("Term 1", start, end, 1L);
		term.setId(id);
		return term;
	}

	private Grade gradeWithId(long id, long examId, BigDecimal marks) {
		Grade grade = Grade.create(1L, 5L, examId, marks, "A", "teacher");
		grade.setId(id);
		return grade;
	}

	private Exam examAt(long id, LocalDateTime scheduledAt) {
		Exam exam = Exam.create("Midterm", 5L, 2L, scheduledAt, BigDecimal.valueOf(100), null);
		exam.setId(id);
		return exam;
	}

	@Test
	@SuppressWarnings("unchecked")
	void generate_withGradeInsideTermRange_includesItAndPersistsReportCard() {
		Student student = studentWithId(1L);
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		Grade grade = gradeWithId(100L, 50L, BigDecimal.valueOf(85));
		Exam exam = examAt(50L, LocalDateTime.of(2026, 2, 1, 9, 0));
		Subject subject = Subject.create("MATH", "Mathematics", null);

		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of(grade));
		when(examRepository.findByIdAndTenantId(50L, 1L)).thenReturn(Optional.of(exam));
		when(subjectRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(subject));
		when(pdfGenerator.generate(eq(student), eq(term), any())).thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(1L, 10L, 1L)).thenReturn(Optional.empty());
		when(reportCardRepository.save(any(ReportCard.class))).thenAnswer(inv -> inv.getArgument(0));

		ReportCard result = reportCardService.generate(1L, 10L);

		ArgumentCaptor<List<ReportCardLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
		verify(pdfGenerator).generate(eq(student), eq(term), linesCaptor.capture());
		assertEquals(1, linesCaptor.getValue().size());
		assertEquals("Mathematics", linesCaptor.getValue().get(0).subjectName());
		assertEquals(10L, result.getTermId());
		assertEquals(1L, result.getStudentId());
		verify(storageService).uploadFile(anyString(), any(byte[].class), eq("application/pdf"));
		verify(eventPublisher).publish(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void generate_withGradeOutsideTermRange_excludesIt() {
		Student student = studentWithId(1L);
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		Grade grade = gradeWithId(100L, 50L, BigDecimal.valueOf(85));
		Exam examOutsideRange = examAt(50L, LocalDateTime.of(2026, 6, 1, 9, 0));

		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of(grade));
		when(examRepository.findByIdAndTenantId(50L, 1L)).thenReturn(Optional.of(examOutsideRange));
		when(pdfGenerator.generate(eq(student), eq(term), any())).thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(1L, 10L, 1L)).thenReturn(Optional.empty());
		when(reportCardRepository.save(any(ReportCard.class))).thenAnswer(inv -> inv.getArgument(0));

		reportCardService.generate(1L, 10L);

		ArgumentCaptor<List<ReportCardLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
		verify(pdfGenerator).generate(eq(student), eq(term), linesCaptor.capture());
		assertTrue(linesCaptor.getValue().isEmpty());
		verify(subjectRepository, never()).findByIdAndTenantId(any(), any());
	}

	@Test
	void generate_whenReportCardAlreadyExistsForTerm_softDeletesPreviousOne() {
		Student student = studentWithId(1L);
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		ReportCard existing = ReportCard.create(1L, 10L, "old-key.pdf");

		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of());
		when(pdfGenerator.generate(eq(student), eq(term), any())).thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(1L, 10L, 1L)).thenReturn(Optional.of(existing));
		when(reportCardRepository.save(any(ReportCard.class))).thenAnswer(inv -> inv.getArgument(0));

		reportCardService.generate(1L, 10L);

		assertTrue(existing.isDeleted());
		verify(reportCardRepository, times(2)).save(any(ReportCard.class));
	}

	@Test
	void generate_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class,
				() -> reportCardService.generate(1L, 10L));
	}
}
