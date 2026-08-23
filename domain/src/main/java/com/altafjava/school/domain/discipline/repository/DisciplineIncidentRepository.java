package com.altafjava.school.domain.discipline.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.discipline.model.DisciplineIncident;

public interface DisciplineIncidentRepository extends JpaRepository<DisciplineIncident, Long> {

	Page<DisciplineIncident> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<DisciplineIncident> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	Optional<DisciplineIncident> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
