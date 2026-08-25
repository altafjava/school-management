package com.altafjava.school.application.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.file.service.StorageService;
import com.altafjava.school.application.certificate.CertificatePdfGenerator;
import com.altafjava.school.application.certificate.CertificateVerificationResult;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.certificate.model.CertificateIssuance;
import com.altafjava.school.domain.certificate.model.CertificateTemplate;
import com.altafjava.school.domain.certificate.repository.CertificateIssuanceRepository;
import com.altafjava.school.domain.certificate.repository.CertificateTemplateRepository;
import com.altafjava.school.domain.certificate.service.CertificatePlaceholderResolver;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves a {@link CertificateTemplate}'s placeholders against a student's actual data, renders a
 * PDF, uploads it, and records the {@link CertificateIssuance}.
 *
 * <p>
 * {@link #issue} is deliberately NOT {@code @Transactional} at the top level, mirroring
 * {@code ReportCardService#generate} exactly (Phase 11's fix): building the PDF is pure computation
 * and {@code storageService.uploadFile} is a network call to S3 — neither should hold a DB
 * transaction (and its connection) open for their duration. The PDF is built and uploaded first;
 * only the DB write runs inside its own short transaction, via {@link #persistIssuance}. If the
 * upload fails, execution never reaches the DB write, so no dangling row is possible. If the DB
 * write fails after a successful upload, the now-orphaned S3 object is best-effort deleted (logged,
 * not swallowed) before the original exception is rethrown.
 */
@Slf4j
@Service
public class CertificateService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int VERIFICATION_CODE_BYTES = 6;
	private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;

	private final CertificateIssuanceRepository certificateIssuanceRepository;
	private final CertificateTemplateRepository certificateTemplateRepository;
	private final StudentRepository studentRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final ClassroomRepository classroomRepository;
	private final AcademicYearRepository academicYearRepository;
	private final StorageService storageService;
	private final CertificatePdfGenerator pdfGenerator;
	private final TransactionTemplate transactionTemplate;

	public CertificateService(CertificateIssuanceRepository certificateIssuanceRepository,
			CertificateTemplateRepository certificateTemplateRepository, StudentRepository studentRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository, ClassroomRepository classroomRepository,
			AcademicYearRepository academicYearRepository, StorageService storageService,
			CertificatePdfGenerator pdfGenerator, PlatformTransactionManager transactionManager) {
		this.certificateIssuanceRepository = certificateIssuanceRepository;
		this.certificateTemplateRepository = certificateTemplateRepository;
		this.studentRepository = studentRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.classroomRepository = classroomRepository;
		this.academicYearRepository = academicYearRepository;
		this.storageService = storageService;
		this.pdfGenerator = pdfGenerator;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Transactional(readOnly = true)
	public Page<CertificateIssuance> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(studentPublicId, tenantId);
		return certificateIssuanceRepository.findByStudentIdAndTenantId(tenantId, student.getId(), pageable);
	}

	@Transactional(readOnly = true)
	public CertificateIssuance findByPublicId(String studentPublicId, String certificatePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		requireStudent(studentPublicId, tenantId);
		return certificateIssuanceRepository.findByPublicIdAndTenantId(UUID.fromString(certificatePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificatePublicId));
	}

	public byte[] downloadPdf(CertificateIssuance issuance) {
		return storageService.downloadFile(issuance.getStorageKey());
	}

	public CertificateIssuance issue(String studentPublicId, String certificateTemplatePublicId,
			Long issuedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(studentPublicId, tenantId);
		CertificateTemplate template = certificateTemplateRepository
				.findByPublicIdAndTenantId(UUID.fromString(certificateTemplatePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Certificate template not found: " + certificateTemplatePublicId));
		if (!template.isActive()) {
			throw new BusinessException("Certificate template is not active: " + template.getName());
		}

		Map<String, String> placeholders = buildPlaceholders(tenantId, student, template);
		String resolvedBody = CertificatePlaceholderResolver.resolve(template.getBodyTemplate(), placeholders);
		byte[] pdf = pdfGenerator.generate(template.getName(), resolvedBody);
		String storageKey = String.format("tenants/%d/certificates/%d/%d/%s.pdf", tenantId, student.getId(),
				template.getId(), UUID.randomUUID());
		storageService.uploadFile(storageKey, pdf, "application/pdf");

		try {
			return persistIssuance(tenantId, student.getId(), template.getId(), storageKey, issuedByUserId);
		} catch (RuntimeException ex) {
			log.error(
					"action=certificate-persist-failed tenantId={} studentId={} templateId={} storageKey={} — cleaning up orphaned upload",
					tenantId, student.getId(), template.getId(), storageKey, ex);
			try {
				storageService.deleteFile(storageKey);
			} catch (RuntimeException cleanupEx) {
				log.error("action=certificate-orphan-cleanup-failed tenantId={} storageKey={}", tenantId, storageKey,
						cleanupEx);
			}
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public CertificateVerificationResult verify(String verificationCode) {
		Long tenantId = TenantContext.getCurrentTenantId();
		CertificateIssuance issuance = certificateIssuanceRepository
				.findByVerificationCodeAndTenantId(verificationCode, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("No certificate found for this verification code"));
		Student student = studentRepository.findByIdAndTenantId(issuance.getStudentId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found for certificate"));
		CertificateTemplate template = certificateTemplateRepository
				.findByIdAndTenantId(issuance.getCertificateTemplateId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Certificate template not found"));
		return new CertificateVerificationResult(student.getFirstName() + " " + student.getLastName(),
				template.getName(), issuance.getIssuedAt());
	}

	private CertificateIssuance persistIssuance(Long tenantId, Long studentId, Long templateId, String storageKey,
			Long issuedByUserId) {
		return transactionTemplate.execute(status -> {
			String verificationCode = generateVerificationCode(tenantId);
			CertificateIssuance issuance = CertificateIssuance.create(studentId, templateId, verificationCode,
					storageKey, issuedByUserId);
			return certificateIssuanceRepository.save(issuance);
		});
	}

	private String generateVerificationCode(Long tenantId) {
		for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
			byte[] randomBytes = new byte[VERIFICATION_CODE_BYTES];
			SECURE_RANDOM.nextBytes(randomBytes);
			String candidate = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
			if (!certificateIssuanceRepository.existsByVerificationCodeAndTenantId(candidate, tenantId)) {
				return candidate;
			}
		}
		throw new BusinessException("Failed to generate a unique certificate verification code — please retry");
	}

	private Student requireStudent(String studentPublicId, Long tenantId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}

	/**
	 * Builds the fixed placeholder set a certificate body may reference. Classroom/academic-year
	 * data is best-effort: a student with no roster assignment yet (e.g. newly enrolled, not yet
	 * placed in a class) still gets a certificate — those tokens simply resolve to an empty string
	 * rather than blocking issuance, since no schema change here can retroactively guarantee a
	 * roster link exists.
	 */
	private Map<String, String> buildPlaceholders(Long tenantId, Student student, CertificateTemplate template) {
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("studentName", student.getFirstName() + " " + student.getLastName());
		placeholders.put("studentCode", student.getStudentCode());
		placeholders.put("certificateName", template.getName());
		placeholders.put("issueDate", LocalDate.now().toString());

		List<StudentClassroomLink> links = studentClassroomLinkRepository.findByStudentId(tenantId,
				student.getId());
		Optional<StudentClassroomLink> currentLink = resolveCurrentLink(tenantId, links);
		currentLink.ifPresentOrElse(link -> {
			Classroom classroom = classroomRepository.findByIdAndTenantId(link.getClassroomId(), tenantId)
					.orElse(null);
			AcademicYear academicYear = academicYearRepository.findByIdAndTenantId(link.getAcademicYearId(), tenantId)
					.orElse(null);
			placeholders.put("className",
					classroom != null ? classroom.getGrade() + " " + classroom.getSection() : "");
			placeholders.put("academicYear", academicYear != null ? academicYear.getName() : "");
		}, () -> {
			placeholders.put("className", "");
			placeholders.put("academicYear", "");
		});

		String admissionDate = links.stream()
				.map(StudentClassroomLink::getEnrolledAt)
				.min(Comparator.naturalOrder())
				.map(Object::toString)
				.orElse("");
		placeholders.put("admissionDate", admissionDate);

		return placeholders;
	}

	private Optional<StudentClassroomLink> resolveCurrentLink(Long tenantId, List<StudentClassroomLink> links) {
		if (links.isEmpty()) {
			return Optional.empty();
		}
		Optional<Long> currentAcademicYearId = academicYearRepository.findByCurrentTrueAndTenantId(tenantId)
				.map(AcademicYear::getId);
		Optional<StudentClassroomLink> currentYearLink = currentAcademicYearId
				.flatMap(yearId -> links.stream().filter(link -> link.getAcademicYearId().equals(yearId)).findFirst());
		return currentYearLink
				.or(() -> links.stream().max(Comparator.comparing(StudentClassroomLink::getEnrolledAt)));
	}
}
