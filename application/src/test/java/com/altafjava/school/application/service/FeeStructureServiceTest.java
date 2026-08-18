package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;

@ExtendWith(MockitoExtension.class)
class FeeStructureServiceTest {

	@Mock
	private FeeStructureRepository feeStructureRepository;

	private FeeStructureService feeStructureService;

	@BeforeEach
	void setUp() {
		feeStructureService = new FeeStructureService(feeStructureRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNewName_succeeds() {
		when(feeStructureRepository.existsByNameAndTenantId("Tuition", 1L)).thenReturn(false);
		when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));

		FeeStructure feeStructure = feeStructureService.create("Tuition", BigDecimal.valueOf(500),
				FeeFrequency.MONTHLY, "Standard");

		assertEquals(BigDecimal.valueOf(500), feeStructure.getAmount());
	}

	@Test
	void create_withDuplicateName_throwsIllegalArgument() {
		when(feeStructureRepository.existsByNameAndTenantId("Tuition", 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> feeStructureService.create("Tuition",
				BigDecimal.valueOf(500), FeeFrequency.MONTHLY, "Standard"));
	}

	@Test
	void findByPublicId_missing_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		when(feeStructureRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> feeStructureService.findByPublicId(publicId.toString()));
	}

	@Test
	void reviseAmount_updatesAmount() {
		UUID publicId = UUID.randomUUID();
		FeeStructure feeStructure = FeeStructure.create("Tuition", BigDecimal.valueOf(500), FeeFrequency.MONTHLY,
				"Standard");
		when(feeStructureRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(feeStructure));
		when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));

		FeeStructure revised = assertDoesNotThrow(
				() -> feeStructureService.reviseAmount(publicId.toString(), BigDecimal.valueOf(600)));

		assertEquals(BigDecimal.valueOf(600), revised.getAmount());
	}
}
