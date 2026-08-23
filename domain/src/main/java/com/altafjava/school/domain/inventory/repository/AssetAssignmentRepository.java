package com.altafjava.school.domain.inventory.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.inventory.model.AssetAssignment;

public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Long> {

	Page<AssetAssignment> findAllByAssetIdAndTenantId(Long assetId, Long tenantId, Pageable pageable);

	Optional<AssetAssignment> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByAssetIdAndTenantIdAndReturnedAtIsNull(Long assetId, Long tenantId);
}
