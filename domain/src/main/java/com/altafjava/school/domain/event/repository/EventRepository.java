package com.altafjava.school.domain.event.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.event.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

	Page<Event> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Event> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Event> findByIdAndTenantId(Long id, Long tenantId);

	long countByTenantIdAndActiveTrueAndEventDateAfter(Long tenantId, LocalDateTime after);
}
