package com.altafjava.school.domain.hostel.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.hostel.model.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Page<Room> findAllByHostelBuildingIdAndTenantId(Long hostelBuildingId, Long tenantId, Pageable pageable);

	Optional<Room> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Room> findByIdAndTenantId(Long id, Long tenantId);
}
