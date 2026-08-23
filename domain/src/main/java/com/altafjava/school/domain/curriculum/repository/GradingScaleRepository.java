package com.altafjava.school.domain.curriculum.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.curriculum.model.GradingScale;

public interface GradingScaleRepository extends JpaRepository<GradingScale, Long> {

	Page<GradingScale> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<GradingScale> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<GradingScale> findByIdAndTenantId(Long id, Long tenantId);

	Optional<GradingScale> findByIsDefaultTrueAndTenantId(Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
