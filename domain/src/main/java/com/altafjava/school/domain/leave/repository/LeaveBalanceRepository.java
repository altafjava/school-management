package com.altafjava.school.domain.leave.repository;

import java.time.LocalDate;
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

	// Drives LeaveCarryForwardExpiryJob — every balance tenant-wide with a carry-forward expiry
	// date that has already passed, regardless of whether any days remain to forfeit (checked by
	// LeaveBalance#forfeitExpiredCarryForward itself).
	List<LeaveBalance> findAllByTenantIdAndCarryForwardExpiresAtLessThanEqual(Long tenantId, LocalDate asOf);
}
