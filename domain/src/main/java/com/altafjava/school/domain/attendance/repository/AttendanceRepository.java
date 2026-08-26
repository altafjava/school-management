package com.altafjava.school.domain.attendance.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	Page<Attendance> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Attendance> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT a FROM Attendance a WHERE a.tenantId = :tenantId AND a.classroomId = :classroomId AND a.attendanceDate = :date")
	List<Attendance> findByClassroomAndDate(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId,
			@Param("date") LocalDate date);

	boolean existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(Long studentId, Long classroomId,
			LocalDate attendanceDate, Long tenantId);

	boolean existsByClassroomIdAndAttendanceDateAndTenantId(Long classroomId, LocalDate attendanceDate,
			Long tenantId);

	Page<Attendance> findByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	// Roster inferred from attendance history — retained for ExamScheduleReminderJob's existing call
	// site; StudentClassroomLinkRepository is now the authoritative source of enrollment, use that.
	@Query("SELECT DISTINCT a.studentId FROM Attendance a WHERE a.tenantId = :tenantId AND a.classroomId = :classroomId")
	List<Long> findDistinctStudentIdsByClassroomId(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId);

	long countByTenantIdAndAttendanceDateBetweenAndStatus(Long tenantId, LocalDate from, LocalDate to,
			AttendanceStatus status);

	long countByTenantIdAndAttendanceDateBetween(Long tenantId, LocalDate from, LocalDate to);

	Page<Attendance> findByClassroomIdInAndTenantId(List<Long> classroomIds, Long tenantId, Pageable pageable);

	long countByStudentIdAndTenantIdAndAttendanceDateBetween(Long studentId, Long tenantId, LocalDate from,
			LocalDate to);

	long countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(Long studentId, Long tenantId, LocalDate from,
			LocalDate to, AttendanceStatus status);

	// Offline-sync delta pulls — @SQLRestriction("deleted = false") means a soft-deleted row is
	// invisible here, so a delete never surfaces as a tombstone via this query; acceptable for now
	// since AttendanceOfflineSyncHandler's own delete() path already reflects the deletion in its
	// synchronous response, only a later independent delta pull would miss it.
	List<Attendance> findByTenantIdAndUpdatedAtAfter(Long tenantId, Instant updatedAt);
}
