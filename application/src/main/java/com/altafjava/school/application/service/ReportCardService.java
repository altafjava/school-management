package com.altafjava.school.application.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.application.branding.TenantBrandingService;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.file.service.StorageService;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
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
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	private final TransactionTemplate transactionTemplate;
	private final TenantRepository tenantRepository;
	private final TenantBrandingService tenantBrandingService;

	public ReportCardService(ReportCardRepository reportCardRepository, StudentRepository studentRepository,
			TermRepository termRepository, GradeRepository gradeRepository, ExamRepository examRepository,
			SubjectRepository subjectRepository, StorageService storageService, ReportCardPdfGenerator pdfGenerator,
			StudentDataAccessGuard studentDataAccessGuard, EventPublisher eventPublisher,
			PlatformTransactionManager transactionManager, TenantRepository tenantRepository,
			TenantBrandingService tenantBrandingService) {
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
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.tenantRepository = tenantRepository;
		this.tenantBrandingService = tenantBrandingService;
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
	 *
	 * <p>
	 * Deliberately NOT {@code @Transactional} at this level. Building the PDF is pure computation
	 * and {@code storageService.uploadFile} is a network call to S3 — neither should hold a DB
	 * transaction (and its connection) open for their duration. The PDF is built and uploaded
	 * first; only the DB write (soft-deleting any prior report card for this term and inserting
	 * the new row) runs inside its own short transaction, via {@link #persistReportCard}. If the
	 * upload fails, execution never reaches the DB write, so no dangling row is possible. If the
	 * DB write fails after a successful upload, the now-orphaned S3 object is best-effort deleted
	 * (logged, not swallowed) before the original exception is rethrown — a failed generation
	 * should surface as a failure to the caller, not silently return a report card that was never
	 * persisted.
	 */
	public ReportCard generate(Long studentId, Long termId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
		Term term = termRepository.findByIdAndTenantId(termId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Term not found: " + termId));

		List<ReportCardLine> lines = buildReportLines(tenantId, studentId, term);
		String tenantName = tenantRepository.findById(tenantId)
				.map(Tenant::getName)
				.orElse("");
		byte[] logoBytes = tenantBrandingService.getLogoBytes(tenantId).orElse(null);

		byte[] pdf = pdfGenerator.generate(student, term, lines, tenantName, logoBytes);
		String storageKey = String.format("tenants/%d/report-cards/%d/%d/%s.pdf", tenantId, studentId, termId,
				UUID.randomUUID());
		storageService.uploadFile(storageKey, pdf, "application/pdf");

		try {
			return persistReportCard(tenantId, studentId, termId, storageKey);
		} catch (RuntimeException ex) {
			log.error(
					"action=report-card-persist-failed tenantId={} studentId={} termId={} storageKey={} — cleaning up orphaned upload",
					tenantId, studentId, termId, storageKey, ex);
			try {
				storageService.deleteFile(storageKey);
			} catch (RuntimeException cleanupEx) {
				log.error("action=report-card-orphan-cleanup-failed tenantId={} storageKey={}", tenantId, storageKey,
						cleanupEx);
			}
			throw ex;
		}
	}

	private ReportCard persistReportCard(Long tenantId, Long studentId, Long termId, String storageKey) {
		return transactionTemplate.execute(status -> {
			reportCardRepository.findByStudentIdAndTermIdAndTenantId(studentId, termId, tenantId)
					.ifPresent(existing -> {
						existing.softDelete("report-card-regeneration");
						reportCardRepository.save(existing);
					});

			ReportCard reportCard = ReportCard.create(studentId, termId, storageKey);
			ReportCard saved = reportCardRepository.save(reportCard);
			eventPublisher.publish(new ReportCardGeneratedEvent(tenantId, studentId, termId, saved.getId()));
			return saved;
		});
	}

	/**
	 * Batches exam and subject lookups instead of issuing one query per Grade row: collect every
	 * distinct examId/subjectId up front, fetch each set with a single {@code IN} query, then
	 * assemble lines from the resulting maps — zero extra queries per row. Grades whose exam falls
	 * outside the term range are dropped before the subject batch is even loaded, preserving the
	 * original short-circuit (no subject lookup wasted on a line that won't be included).
	 */
	private List<ReportCardLine> buildReportLines(Long tenantId, Long studentId, Term term) {
		LocalDateTime termStart = term.getStartDate().atStartOfDay();
		LocalDateTime termEnd = term.getEndDate().atTime(LocalTime.MAX);

		List<Grade> grades = gradeRepository.findByStudentId(tenantId, studentId);
		Map<Long, Exam> examsById = loadExamsById(tenantId, grades);

		List<Grade> gradesInTerm = grades.stream()
				.filter(grade -> isWithinTerm(examsById.get(grade.getExamId()), termStart, termEnd))
				.toList();
		Map<Long, Subject> subjectsById = loadSubjectsById(tenantId, gradesInTerm);

		return gradesInTerm.stream()
				.map(grade -> toLine(grade, examsById.get(grade.getExamId()), subjectsById))
				.toList();
	}

	private boolean isWithinTerm(Exam exam, LocalDateTime termStart, LocalDateTime termEnd) {
		return exam != null && !exam.getScheduledAt().isBefore(termStart) && !exam.getScheduledAt().isAfter(termEnd);
	}

	private Map<Long, Exam> loadExamsById(Long tenantId, List<Grade> grades) {
		List<Long> examIds = grades.stream().map(Grade::getExamId).distinct().toList();
		if (examIds.isEmpty()) {
			return Map.of();
		}
		return examRepository.findAllByIdInAndTenantId(examIds, tenantId).stream()
				.collect(Collectors.toMap(Exam::getId, Function.identity()));
	}

	private Map<Long, Subject> loadSubjectsById(Long tenantId, List<Grade> grades) {
		List<Long> subjectIds = grades.stream().map(Grade::getSubjectId).distinct().toList();
		if (subjectIds.isEmpty()) {
			return Map.of();
		}
		return subjectRepository.findAllByIdInAndTenantId(subjectIds, tenantId).stream()
				.collect(Collectors.toMap(Subject::getId, Function.identity()));
	}

	private ReportCardLine toLine(Grade grade, Exam exam, Map<Long, Subject> subjectsById) {
		String subjectName = Optional.ofNullable(subjectsById.get(grade.getSubjectId()))
				.map(Subject::getName)
				.orElse("Unknown Subject");
		return new ReportCardLine(subjectName, exam.getTitle(), grade.getMarks(), exam.getMaxMarks(),
				grade.getGradeLetter());
	}
}
