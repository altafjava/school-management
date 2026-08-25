package com.altafjava.school.domain.payroll.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.payroll.model.Payslip;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

	boolean existsByTeacherIdAndPayYearAndPayMonthAndTenantId(Long teacherId, int payYear, int payMonth,
			Long tenantId);

	Page<Payslip> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<Payslip> findAllByTeacherIdAndTenantId(Long teacherId, Long tenantId, Pageable pageable);

	Optional<Payslip> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
