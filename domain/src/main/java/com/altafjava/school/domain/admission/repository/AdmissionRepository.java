package com.altafjava.school.domain.admission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionStatus;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

	Page<Admission> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Admission> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Admission> findByIdAndTenantId(Long id, Long tenantId);

	Optional<Admission> findByEnrollmentSagaIdAndTenantId(UUID sagaId, Long tenantId);

	// Merit-list generation input: every scored, still-undecided applicant for one grade cohort.
	List<Admission> findAllByTenantIdAndAppliedGradeAndStatusAndEntranceTestScoreIsNotNull(Long tenantId,
			String appliedGrade, AdmissionStatus status);
}
