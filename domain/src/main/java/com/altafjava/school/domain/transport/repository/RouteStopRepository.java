package com.altafjava.school.domain.transport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.transport.model.RouteStop;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

	List<RouteStop> findAllByRouteIdAndTenantIdOrderBySequenceOrderAsc(Long routeId, Long tenantId);

	Optional<RouteStop> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<RouteStop> findByIdAndTenantId(Long id, Long tenantId);
}
