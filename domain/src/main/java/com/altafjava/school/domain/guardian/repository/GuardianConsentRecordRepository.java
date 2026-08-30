package com.altafjava.school.domain.guardian.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.guardian.model.GuardianConsentRecord;
import com.altafjava.school.domain.guardian.model.GuardianConsentType;

public interface GuardianConsentRecordRepository extends JpaRepository<GuardianConsentRecord, Long> {

	Optional<GuardianConsentRecord> findByGuardianIdAndStudentIdAndConsentTypeAndTenantId(Long guardianId,
			Long studentId, GuardianConsentType consentType, Long tenantId);

	List<GuardianConsentRecord> findAllByStudentIdAndTenantId(Long studentId, Long tenantId);

	List<GuardianConsentRecord> findAllByGuardianIdAndStudentIdAndTenantId(Long guardianId, Long studentId,
			Long tenantId);
}
