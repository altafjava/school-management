package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.inventory.model.AssetAssignment;
import com.altafjava.school.domain.inventory.model.AssetStatus;
import com.altafjava.school.domain.inventory.model.AssignedToType;
import com.altafjava.school.domain.inventory.repository.AssetAssignmentRepository;
import com.altafjava.school.domain.inventory.repository.AssetRepository;

@ExtendWith(MockitoExtension.class)
class AssetAssignmentServiceTest {

	private static final UUID ASSET_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private AssetAssignmentRepository assetAssignmentRepository;
	@Mock
	private AssetRepository assetRepository;

	private AssetAssignmentService assetAssignmentService;

	@BeforeEach
	void setUp() {
		assetAssignmentService = new AssetAssignmentService(assetAssignmentRepository, assetRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Asset assetWithId(long id) {
		Asset asset = Asset.create("AST-1", "Projector", "Electronics", LocalDate.of(2026, 1, 1),
				BigDecimal.valueOf(500), "Room 101");
		asset.setId(id);
		return asset;
	}

	@Test
	void assign_marksAssetInUse() {
		Asset asset = assetWithId(5L);
		when(assetRepository.findByPublicIdAndTenantId(ASSET_PUBLIC_ID, 1L)).thenReturn(Optional.of(asset));
		when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
		when(assetAssignmentRepository.save(any(AssetAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

		AssetAssignment assignment = assertDoesNotThrow(() -> assetAssignmentService.assign(
				ASSET_PUBLIC_ID.toString(), AssignedToType.STAFF, 20L, LocalDate.of(2026, 4, 1)));

		assertEquals(AssetStatus.IN_USE, asset.getStatus());
		assertEquals(5L, assignment.getAssetId());
	}

	@Test
	void markReturned_marksAssetAvailableAgain() {
		UUID assignmentPublicId = UUID.randomUUID();
		Asset asset = assetWithId(5L);
		asset.markInUse();
		AssetAssignment assignment = AssetAssignment.create(5L, AssignedToType.STAFF, 20L, LocalDate.of(2026, 4, 1));
		when(assetAssignmentRepository.findByPublicIdAndTenantId(assignmentPublicId, 1L))
				.thenReturn(Optional.of(assignment));
		when(assetAssignmentRepository.save(any(AssetAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
		when(assetRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(asset));
		when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

		assetAssignmentService.markReturned(assignmentPublicId.toString(), LocalDate.of(2026, 5, 1));

		assertEquals(AssetStatus.AVAILABLE, asset.getStatus());
	}
}
