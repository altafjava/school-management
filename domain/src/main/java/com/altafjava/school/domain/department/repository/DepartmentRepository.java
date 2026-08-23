package com.altafjava.school.domain.department.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.department.model.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	Page<Department> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Department> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Department> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
