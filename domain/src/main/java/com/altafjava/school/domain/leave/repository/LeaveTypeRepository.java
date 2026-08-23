package com.altafjava.school.domain.leave.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.leave.model.LeaveType;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

	Page<LeaveType> findAllByTenantId(Long tenantId, Pageable pageable);

	List<LeaveType> findAllByTenantIdAndActiveTrue(Long tenantId);

	Optional<LeaveType> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<LeaveType> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
