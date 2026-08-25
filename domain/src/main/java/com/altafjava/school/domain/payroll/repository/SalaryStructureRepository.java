package com.altafjava.school.domain.payroll.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.payroll.model.SalaryStructure;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

	Optional<SalaryStructure> findByTeacherIdAndActiveTrueAndTenantId(Long teacherId, Long tenantId);

	Page<SalaryStructure> findAllByTeacherIdAndTenantId(Long teacherId, Long tenantId, Pageable pageable);

	Optional<SalaryStructure> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
