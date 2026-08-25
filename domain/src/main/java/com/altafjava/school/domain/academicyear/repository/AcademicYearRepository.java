package com.altafjava.school.domain.academicyear.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.academicyear.model.AcademicYear;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

	Page<AcademicYear> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<AcademicYear> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<AcademicYear> findByCurrentTrueAndTenantId(Long tenantId);

	Optional<AcademicYear> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);
}
