package com.altafjava.school.domain.alumni.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.alumni.model.AlumniProfile;

public interface AlumniProfileRepository extends JpaRepository<AlumniProfile, Long> {

	Page<AlumniProfile> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<AlumniProfile> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentIdAndTenantId(Long studentId, Long tenantId);
}
