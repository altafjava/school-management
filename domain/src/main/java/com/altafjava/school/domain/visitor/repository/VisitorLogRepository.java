package com.altafjava.school.domain.visitor.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.visitor.model.VisitorLog;

public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {

	Page<VisitorLog> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<VisitorLog> findAllByTenantIdAndCheckOutAtIsNull(Long tenantId, Pageable pageable);

	Page<VisitorLog> findAllByTenantIdAndCheckInAtBetween(Long tenantId, LocalDateTime from, LocalDateTime to,
			Pageable pageable);

	Optional<VisitorLog> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
