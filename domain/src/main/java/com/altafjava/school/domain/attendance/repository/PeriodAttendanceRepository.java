package com.altafjava.school.domain.attendance.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;

public interface PeriodAttendanceRepository extends JpaRepository<PeriodAttendance, Long> {

	Page<PeriodAttendance> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<PeriodAttendance> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Page<PeriodAttendance> findByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	boolean existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(Long studentId, Long timetableEntryId,
			LocalDate attendanceDate, Long tenantId);
}
