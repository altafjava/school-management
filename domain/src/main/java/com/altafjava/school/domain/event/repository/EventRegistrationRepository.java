package com.altafjava.school.domain.event.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.event.model.EventRegistration;
import com.altafjava.school.domain.event.model.EventRegistrationStatus;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

	Page<EventRegistration> findAllByEventIdAndTenantId(Long eventId, Long tenantId, Pageable pageable);

	Optional<EventRegistration> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	long countByEventIdAndTenantIdAndStatus(Long eventId, Long tenantId, EventRegistrationStatus status);

	boolean existsByEventIdAndStudentIdAndTenantIdAndStatus(Long eventId, Long studentId, Long tenantId,
			EventRegistrationStatus status);
}
