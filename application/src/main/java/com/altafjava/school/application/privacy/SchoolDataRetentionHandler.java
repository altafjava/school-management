package com.altafjava.school.application.privacy;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.core.privacy.DomainRetentionHandler;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * school-saas's side of the FERPA/COPPA/GDPR-K/DPDP retention-window extension point — see
 * {@link DomainRetentionHandler}, wired via {@code SchoolPlatformConfigurer#domainRetentionHandler()}.
 * Only students who have already left the school ({@link EnrollmentStatus#WITHDRAWN},
 * {@link EnrollmentStatus#GRADUATED}, {@link EnrollmentStatus#TRANSFERRED}) are ever in scope — an
 * {@code ACTIVE} or {@code SUSPENDED} student's data is still in active use regardless of how a
 * tenant's {@code entityType=STUDENT} policy is configured.
 *
 * <p>
 * Each student is anonymized in its own transaction ({@link TransactionTemplate}, the same
 * pattern {@code CertificateService} uses for the same self-invocation reason —
 * {@code @Transactional} only applies through the Spring proxy, which a private helper method
 * called from within this class would bypass). A batch-wide single transaction would let one
 * student with a data problem (a bad row, a constraint violation) roll back every other, otherwise
 * healthy student in the same tenant's sweep — and since the failing row would still match the
 * same query on every subsequent day's run, it would permanently block that tenant's retention
 * enforcement until someone manually intervened, with no per-tenant signal beyond a log line.
 */
@Slf4j
@Component
public class SchoolDataRetentionHandler implements DomainRetentionHandler {

	private static final String ENTITY_TYPE_STUDENT = "STUDENT";
	private static final List<EnrollmentStatus> INACTIVE_STATUSES = List.of(EnrollmentStatus.WITHDRAWN,
			EnrollmentStatus.GRADUATED, EnrollmentStatus.TRANSFERRED);

	private final StudentRepository studentRepository;
	private final TransactionTemplate transactionTemplate;

	public SchoolDataRetentionHandler(StudentRepository studentRepository,
			PlatformTransactionManager transactionManager) {
		this.studentRepository = studentRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	public int enforceRetention(Long tenantId, String entityType, Instant cutoff, String deletionPolicy) {
		if (!ENTITY_TYPE_STUDENT.equals(entityType)) {
			log.info("action=retention-entity-type-not-handled tenantId={} entityType={}", tenantId, entityType);
			return 0;
		}

		List<Student> eligible = studentRepository
				.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(tenantId,
						INACTIVE_STATUSES, cutoff);

		int succeeded = 0;
		int failed = 0;
		for (Student student : eligible) {
			if (anonymizeInOwnTransaction(tenantId, student.getId(), deletionPolicy)) {
				succeeded++;
			} else {
				failed++;
			}
		}

		if (failed > 0) {
			// Thrown after processing every student rather than mid-loop, so the healthy
			// students above already committed in their own transactions — only the count
			// reported back to DataRetentionEnforcementScheduler undercounts on a partial-failure
			// day (it logs this as a failed policy run and moves on); the actual anonymizations
			// are not lost.
			throw new IllegalStateException(
					failed + " of " + eligible.size() + " student retention actions failed for tenant " + tenantId
							+ " — see preceding per-student error logs");
		}
		return succeeded;
	}

	private boolean anonymizeInOwnTransaction(Long tenantId, Long studentId, String deletionPolicy) {
		try {
			transactionTemplate.executeWithoutResult(status -> {
				Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
						.orElseThrow(() -> new IllegalStateException("Student not found: " + studentId));
				applyDeletionPolicy(student, deletionPolicy);
				studentRepository.save(student);
			});
			return true;
		} catch (Exception e) {
			log.error("action=retention-student-failed tenantId={} studentId={}", tenantId, studentId, e);
			return false;
		}
	}

	private void applyDeletionPolicy(Student student, String deletionPolicy) {
		switch (deletionPolicy) {
			case "ANONYMIZE" -> {
				student.erasePii();
				student.softDelete("data-retention-window-elapsed");
			}
			case "SOFT_DELETE" -> student.softDelete("data-retention-window-elapsed");
			default -> throw new UnsupportedOperationException(
					"Student retention does not support deletionPolicy=" + deletionPolicy
							+ " — Student is referenced by attendance/grades/fee records a hard delete would orphan; use ANONYMIZE or SOFT_DELETE");
		}
	}
}
