package com.altafjava.school.domain.transport.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.transport.model.TransportAssignment;

public interface TransportAssignmentRepository extends JpaRepository<TransportAssignment, Long> {

	Page<TransportAssignment> findAllByRouteIdAndTenantId(Long routeId, Long tenantId, Pageable pageable);

	Optional<TransportAssignment> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentIdAndTenantIdAndEffectiveToIsNull(Long studentId, Long tenantId);
}
