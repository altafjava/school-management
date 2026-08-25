package com.altafjava.school.domain.health.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.health.model.HealthRecord;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

	Optional<HealthRecord> findByStudentIdAndTenantId(Long studentId, Long tenantId);

	Optional<HealthRecord> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
