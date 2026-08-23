package com.altafjava.school.domain.transport.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.transport.model.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

	Page<Route> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Route> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Route> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
