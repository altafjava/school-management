package com.altafjava.school.domain.leave.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.leave.model.LeaveBalance;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

	List<LeaveBalance> findAllByTeacherIdAndAcademicYearIdAndTenantId(Long teacherId, Long academicYearId,
			Long tenantId);

	Optional<LeaveBalance> findByTeacherIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(Long teacherId,
			Long leaveTypeId, Long academicYearId, Long tenantId);

	boolean existsByTeacherIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(Long teacherId, Long leaveTypeId,
			Long academicYearId, Long tenantId);
}
