package com.altafjava.school.domain.attendance.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.attendance.model.AttendanceCorrection;

public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection, Long> {

	@Query("SELECT c FROM AttendanceCorrection c WHERE c.tenantId = :tenantId AND c.attendanceId = :attendanceId "
			+ "ORDER BY c.createdAt DESC")
	Page<AttendanceCorrection> findByAttendanceIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("attendanceId") Long attendanceId, Pageable pageable);
}
