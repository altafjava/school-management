package com.altafjava.school.application.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.file.service.StorageService;
import com.altafjava.school.application.reportcard.ReportCardLine;
import com.altafjava.school.application.reportcard.ReportCardPdfGenerator;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.reportcard.event.ReportCardGeneratedEvent;
import com.altafjava.school.domain.reportcard.model.ReportCard;
import com.altafjava.school.domain.reportcard.repository.ReportCardRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@Service
public class ReportCardService {

	private final ReportCardRepository reportCardRepository;
	private final StudentRepository studentRepository;
	private final TermRepository termRepository;
	private final GradeRepository gradeRepository;
	private final ExamRepository examRepository;
	private final SubjectRepository subjectRepository;
	private final StorageService storageService;
	private final ReportCardPdfGenerator pdfGenerator;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final EventPublisher eventPublisher;

	public ReportCardService(ReportCardRepository reportCardRepository, StudentRepository studentRepository,
			TermRepository termRepository, GradeRepository gradeRepository, ExamRepository examRepository,
			SubjectRepository subjectRepository, StorageService storageService, ReportCardPdfGenerator pdfGenerator,
			StudentDataAccessGuard studentDataAccessGuard, EventPublisher eventPublisher) {
		this.reportCardRepository = reportCardRepository;
		this.studentRepository = studentRepository;
		this.termRepository = termRepository;
		this.gradeRepository = gradeRepository;
		this.examRepository = examRepository;
		this.subjectRepository = subjectRepository;
		this.storageService = storageService;
		this.pdfGenerator = pdfGenerator;
		this.studentDataAccessGuard = studentDataAccessGuard;
		this.eventPublisher = eventPublisher;
	}

	@Transactional(readOnly = true)
	public Page<ReportCard> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return reportCardRepository.findByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public ReportCard findByPublicId(String studentPublicId, String reportCardPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return reportCardRepository.findByPublicIdAndTenantId(UUID.fromString(reportCardPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Report card not found: " + reportCardPublicId));
	}

	public byte[] downloadPdf(ReportCard reportCard) {
		return storageService.downloadFile(reportCard.getStorageKey());
	}

	/**
	 * Generates (or regenerates) a student's report card for a term: aggregates every Grade
	 * whose Exam falls within the term's date range, renders a PDF, and stores it. There is no
	 * {@code termId} column on {@code Exam} — the term boundary is applied as a date-range filter
	 * on {@code Exam.scheduledAt} against {@code Term.startDate}/{@code endDate} instead, which
	 * avoids a schema change to an entity two earlier phases already shipped and tested.
	 */
	@Transactional
	public ReportCard generate(Long studentId, Long termId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
		Term term = termRepository.findByIdAndTenantId(termId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Term not found: " + termId));

		List<ReportCardLine> lines = buildReportLines(tenantId, studentId, term);

		byte[] pdf = pdfGenerator.generate(student, term, lines);
		String storageKey = String.format("tenants/%d/report-cards/%d/%d/%s.pdf", tenantId, studentId, termId,
				UUID.randomUUID());
		storageService.uploadFile(storageKey, pdf, "application/pdf");

		reportCardRepository.findByStudentIdAndTermIdAndTenantId(studentId, termId, tenantId)
				.ifPresent(existing -> {
					existing.softDelete("report-card-regeneration");
					reportCardRepository.save(existing);
				});

		ReportCard reportCard = ReportCard.create(studentId, termId, storageKey);
		ReportCard saved = reportCardRepository.save(reportCard);
		eventPublisher.publish(new ReportCardGeneratedEvent(tenantId, studentId, termId, saved.getId()));
		return saved;
	}

	private List<ReportCardLine> buildReportLines(Long tenantId, Long studentId, Term term) {
		LocalDateTime termStart = term.getStartDate().atStartOfDay();
		LocalDateTime termEnd = term.getEndDate().atTime(LocalTime.MAX);

		return gradeRepository.findByStudentId(tenantId, studentId).stream()
				.map(grade -> toLine(tenantId, grade, termStart, termEnd))
				.filter(Objects::nonNull)
				.toList();
	}

	private ReportCardLine toLine(Long tenantId, Grade grade, LocalDateTime termStart, LocalDateTime termEnd) {
		Exam exam = examRepository.findByIdAndTenantId(grade.getExamId(), tenantId).orElse(null);
		if (exam == null || exam.getScheduledAt().isBefore(termStart) || exam.getScheduledAt().isAfter(termEnd)) {
			return null;
		}
		String subjectName = subjectRepository.findByIdAndTenantId(grade.getSubjectId(), tenantId)
				.map(subject -> subject.getName())
				.orElse("Unknown Subject");
		return new ReportCardLine(subjectName, exam.getTitle(), grade.getMarks(), exam.getMaxMarks(),
				grade.getGradeLetter());
	}
}
