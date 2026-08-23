package com.altafjava.school.domain.leave.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

	Page<LeaveRequest> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<LeaveRequest> findAllByTeacherIdAndTenantId(Long teacherId, Long tenantId, Pageable pageable);

	Optional<LeaveRequest> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	long countByTenantIdAndStatus(Long tenantId, LeaveRequestStatus status);
}
