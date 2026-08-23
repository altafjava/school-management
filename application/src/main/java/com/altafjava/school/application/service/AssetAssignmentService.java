package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.inventory.model.AssetAssignment;
import com.altafjava.school.domain.inventory.model.AssignedToType;
import com.altafjava.school.domain.inventory.repository.AssetAssignmentRepository;
import com.altafjava.school.domain.inventory.repository.AssetRepository;

@Service
public class AssetAssignmentService {

	private final AssetAssignmentRepository assetAssignmentRepository;
	private final AssetRepository assetRepository;

	public AssetAssignmentService(AssetAssignmentRepository assetAssignmentRepository,
			AssetRepository assetRepository) {
		this.assetAssignmentRepository = assetAssignmentRepository;
		this.assetRepository = assetRepository;
	}

	@Transactional(readOnly = true)
	public Page<AssetAssignment> listForAsset(String assetPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Asset asset = assetRepository.findByPublicIdAndTenantId(UUID.fromString(assetPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetPublicId));
		return assetAssignmentRepository.findAllByAssetIdAndTenantId(asset.getId(), tenantId, pageable);
	}

	@Transactional
	public AssetAssignment assign(String assetPublicId, AssignedToType assignedToType, Long assignedToId,
			LocalDate assignedAt) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Asset asset = assetRepository.findByPublicIdAndTenantId(UUID.fromString(assetPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetPublicId));
		asset.markInUse();
		assetRepository.save(asset);
		AssetAssignment assignment = AssetAssignment.create(asset.getId(), assignedToType, assignedToId, assignedAt);
		return assetAssignmentRepository.save(assignment);
	}

	@Transactional
	public AssetAssignment markReturned(String publicId, LocalDate returnedAt) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AssetAssignment assignment = assetAssignmentRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset assignment not found: " + publicId));
		assignment.markReturned(returnedAt);
		AssetAssignment saved = assetAssignmentRepository.save(assignment);

		Asset asset = assetRepository.findByIdAndTenantId(assignment.getAssetId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assignment.getAssetId()));
		asset.markAvailable();
		assetRepository.save(asset);
		return saved;
	}
}
