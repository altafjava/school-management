package com.altafjava.school.domain.guardian.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.guardian.model.Guardian;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

	Page<Guardian> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Guardian> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Guardian> findByUserIdAndTenantId(Long userId, Long tenantId);

	// A "pending, unclaimed" guardian record — created by an admin/saga ahead of the guardian
	// ever logging in — that self-registration can claim by matching email.
	Optional<Guardian> findByEmailAndTenantIdAndUserIdIsNull(String email, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	Optional<Guardian> findByIdAndTenantId(Long id, Long tenantId);
}
