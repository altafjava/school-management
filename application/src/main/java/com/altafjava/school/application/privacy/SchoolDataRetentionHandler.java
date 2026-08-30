package com.altafjava.school.application.privacy;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
 */
@Slf4j
@Component
public class SchoolDataRetentionHandler implements DomainRetentionHandler {

	private static final String ENTITY_TYPE_STUDENT = "STUDENT";
	private static final List<EnrollmentStatus> INACTIVE_STATUSES = List.of(EnrollmentStatus.WITHDRAWN,
			EnrollmentStatus.GRADUATED, EnrollmentStatus.TRANSFERRED);

	private final StudentRepository studentRepository;

	public SchoolDataRetentionHandler(StudentRepository studentRepository) {
		this.studentRepository = studentRepository;
	}

	@Override
	@Transactional
	public int enforceRetention(Long tenantId, String entityType, Instant cutoff, String deletionPolicy) {
		if (!ENTITY_TYPE_STUDENT.equals(entityType)) {
			log.info("action=retention-entity-type-not-handled tenantId={} entityType={}", tenantId, entityType);
			return 0;
		}

		List<Student> eligible = studentRepository
				.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(tenantId,
						INACTIVE_STATUSES, cutoff);

		for (Student student : eligible) {
			applyDeletionPolicy(student, deletionPolicy);
			studentRepository.save(student);
		}
		return eligible.size();
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
