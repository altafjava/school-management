package com.altafjava.school.domain.inventory.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.inventory.model.Asset;

public interface AssetRepository extends JpaRepository<Asset, Long> {

	Page<Asset> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Asset> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Asset> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByAssetCodeAndTenantId(String assetCode, Long tenantId);
}
