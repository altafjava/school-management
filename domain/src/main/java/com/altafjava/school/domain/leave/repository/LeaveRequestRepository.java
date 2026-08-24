package com.altafjava.school.domain.leave.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

	Page<LeaveRequest> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<LeaveRequest> findAllByTeacherIdAndTenantId(Long teacherId, Long tenantId, Pageable pageable);

	Optional<LeaveRequest> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	long countByTenantIdAndStatus(Long tenantId, LeaveRequestStatus status);

	// Monthly leave-utilization trend (see LeaveUtilizationTrendDataProvider) — summed at the DB
	// per period rather than pulled row-by-row.
	@Query("SELECT COALESCE(SUM(lr.daysRequested), 0) FROM LeaveRequest lr WHERE lr.tenantId = :tenantId "
			+ "AND lr.status = :status AND lr.startDate BETWEEN :from AND :to")
	BigDecimal sumDaysRequestedByTenantIdAndStatusAndStartDateBetween(@Param("tenantId") Long tenantId,
			@Param("status") LeaveRequestStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
