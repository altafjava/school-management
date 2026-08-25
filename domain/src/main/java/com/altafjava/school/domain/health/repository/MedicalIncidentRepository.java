package com.altafjava.school.domain.health.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.health.model.MedicalIncident;

public interface MedicalIncidentRepository extends JpaRepository<MedicalIncident, Long> {

	Page<MedicalIncident> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<MedicalIncident> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	Optional<MedicalIncident> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
