package com.altafjava.school.domain.hostel.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.hostel.model.RoomAllocation;

public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, Long> {

	Page<RoomAllocation> findAllByRoomIdAndTenantId(Long roomId, Long tenantId, Pageable pageable);

	Optional<RoomAllocation> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	long countByRoomIdAndTenantIdAndAllocatedUntilIsNull(Long roomId, Long tenantId);

	boolean existsByStudentIdAndTenantIdAndAllocatedUntilIsNull(Long studentId, Long tenantId);
}
