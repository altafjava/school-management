package com.altafjava.school.domain.hostel.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.hostel.model.HostelBuilding;

public interface HostelBuildingRepository extends JpaRepository<HostelBuilding, Long> {

	Page<HostelBuilding> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<HostelBuilding> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
