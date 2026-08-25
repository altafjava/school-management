package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.customfield.model.CustomFieldDefinition;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.customfield.model.CustomFieldType;
import com.altafjava.school.domain.customfield.repository.CustomFieldDefinitionRepository;

@ExtendWith(MockitoExtension.class)
class CustomFieldDefinitionServiceTest {

	@Mock
	private CustomFieldDefinitionRepository customFieldDefinitionRepository;

	private CustomFieldDefinitionService customFieldDefinitionService;

	@BeforeEach
	void setUp() {
		customFieldDefinitionService = new CustomFieldDefinitionService(customFieldDefinitionRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNewFieldKey_succeeds() {
		when(customFieldDefinitionRepository.existsByTenantIdAndEntityTypeAndFieldKey(1L,
				CustomFieldEntityType.STUDENT, "bloodGroup")).thenReturn(false);
		when(customFieldDefinitionRepository.save(any(CustomFieldDefinition.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CustomFieldDefinition definition = customFieldDefinitionService.create(CustomFieldEntityType.STUDENT,
				"bloodGroup", "Blood Group", CustomFieldType.TEXT, false);

		assertEquals("bloodGroup", definition.getFieldKey());
	}

	@Test
	void create_withDuplicateFieldKey_throwsBusinessException() {
		when(customFieldDefinitionRepository.existsByTenantIdAndEntityTypeAndFieldKey(1L,
				CustomFieldEntityType.STUDENT, "bloodGroup")).thenReturn(true);

		assertThrows(BusinessException.class, () -> customFieldDefinitionService.create(CustomFieldEntityType.STUDENT,
				"bloodGroup", "Blood Group", CustomFieldType.TEXT, false));
	}

	@Test
	void findByPublicId_withUnknownId_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		when(customFieldDefinitionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> customFieldDefinitionService.findByPublicId(publicId.toString()));
	}

	@Test
	void deactivate_flipsActiveFlagOnResolvedDefinition() {
		CustomFieldDefinition definition = CustomFieldDefinition.create(CustomFieldEntityType.STUDENT, "allergy",
				"Allergy", CustomFieldType.TEXT, false);
		UUID publicId = UUID.randomUUID();
		definition.setPublicId(publicId);
		when(customFieldDefinitionRepository.findByPublicIdAndTenantId(publicId, 1L))
				.thenReturn(Optional.of(definition));
		when(customFieldDefinitionRepository.save(any(CustomFieldDefinition.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CustomFieldDefinition result = customFieldDefinitionService.deactivate(publicId.toString());

		assertEquals(false, result.isActive());
	}
}
