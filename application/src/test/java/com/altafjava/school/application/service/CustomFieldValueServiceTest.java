package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.extension.EntityAttributeService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.customfield.CustomFieldValue;
import com.altafjava.school.domain.customfield.model.CustomFieldDefinition;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.customfield.model.CustomFieldType;
import com.altafjava.school.domain.customfield.model.CustomFieldValidationRule;
import com.altafjava.school.domain.customfield.repository.CustomFieldDefinitionRepository;

@ExtendWith(MockitoExtension.class)
class CustomFieldValueServiceTest {

	@Mock
	private CustomFieldDefinitionRepository customFieldDefinitionRepository;
	@Mock
	private EntityAttributeService entityAttributeService;

	private CustomFieldValueService customFieldValueService;

	@BeforeEach
	void setUp() {
		customFieldValueService = new CustomFieldValueService(customFieldDefinitionRepository,
				entityAttributeService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private CustomFieldDefinition activeDefinition(String fieldKey, CustomFieldType type, boolean required) {
		return CustomFieldDefinition.create(CustomFieldEntityType.STUDENT, fieldKey, fieldKey, type, required);
	}

	@Test
	void setValue_withActiveTextDefinition_delegatesToEntityAttributeService() {
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"nickname")).thenReturn(Optional.of(activeDefinition("nickname", CustomFieldType.TEXT, false)));

		customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "nickname", "Ali");

		verify(entityAttributeService).setAttribute(1L, 42L, "STUDENT", "school-custom-fields", "nickname", "Ali");
	}

	@Test
	void setValue_withUnknownFieldKey_throwsBusinessException() {
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"unknown")).thenReturn(Optional.empty());

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "unknown", "x"));
	}

	@Test
	void setValue_withInactiveDefinition_throwsBusinessException() {
		CustomFieldDefinition definition = activeDefinition("retired", CustomFieldType.TEXT, false);
		definition.deactivate();
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"retired")).thenReturn(Optional.of(definition));

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "retired", "x"));
	}

	@Test
	void setValue_withNonNumericValueForNumberField_throwsBusinessException() {
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"height")).thenReturn(Optional.of(activeDefinition("height", CustomFieldType.NUMBER, false)));

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "height", "tall"));
	}

	@Test
	void setValue_withInvalidDateForDateField_throwsBusinessException() {
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"joinedOn")).thenReturn(Optional.of(activeDefinition("joinedOn", CustomFieldType.DATE, false)));

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "joinedOn", "not-a-date"));
	}

	@Test
	void setValue_withNonBooleanValueForBooleanField_throwsBusinessException() {
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"vaccinated")).thenReturn(Optional.of(activeDefinition("vaccinated", CustomFieldType.BOOLEAN, false)));

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "vaccinated", "maybe"));
	}

	@Test
	void setValue_withBlankValueForRequiredField_throwsBusinessException() {
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"emergencyContact"))
				.thenReturn(Optional.of(activeDefinition("emergencyContact", CustomFieldType.TEXT, true)));

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "emergencyContact", ""));
	}

	private CustomFieldDefinition definitionWithRule(String fieldKey, CustomFieldType type,
			CustomFieldValidationRule rule) {
		CustomFieldDefinition definition = activeDefinition(fieldKey, type, false);
		definition.updateValidationRule(rule);
		return definition;
	}

	@Test
	void setValue_withNumberBelowMin_throwsBusinessException() {
		CustomFieldValidationRule rule = CustomFieldValidationRule.builder()
				.minValue(java.math.BigDecimal.TEN).build();
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"score")).thenReturn(Optional.of(definitionWithRule("score", CustomFieldType.NUMBER, rule)));

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "score", "5"));
	}

	@Test
	void setValue_withNumberWithinMinMax_succeeds() {
		CustomFieldValidationRule rule = CustomFieldValidationRule.builder()
				.minValue(java.math.BigDecimal.ONE).maxValue(java.math.BigDecimal.TEN).build();
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"score")).thenReturn(Optional.of(definitionWithRule("score", CustomFieldType.NUMBER, rule)));

		customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "score", "7");

		verify(entityAttributeService).setAttribute(1L, 42L, "STUDENT", "school-custom-fields", "score", "7");
	}

	@Test
	void setValue_withTextNotMatchingRegex_throwsBusinessException() {
		CustomFieldValidationRule rule = CustomFieldValidationRule.builder().regexPattern("^[A-Z]{2}\\d{4}$").build();
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"badgeCode")).thenReturn(Optional.of(definitionWithRule("badgeCode", CustomFieldType.TEXT, rule)));

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "badgeCode", "abc"));
	}

	@Test
	void setValue_withSelectOptionInList_succeeds() {
		CustomFieldValidationRule rule = CustomFieldValidationRule.builder().options("Red,Blue,Green").build();
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"houseColor")).thenReturn(Optional.of(definitionWithRule("houseColor", CustomFieldType.SELECT, rule)));

		customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "houseColor", "Blue");

		verify(entityAttributeService).setAttribute(1L, 42L, "STUDENT", "school-custom-fields", "houseColor", "Blue");
	}

	@Test
	void setValue_withSelectOptionNotInList_throwsBusinessException() {
		CustomFieldValidationRule rule = CustomFieldValidationRule.builder().options("Red,Blue").build();
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"houseColor")).thenReturn(Optional.of(definitionWithRule("houseColor", CustomFieldType.SELECT, rule)));

		assertThrows(BusinessException.class,
				() -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "houseColor", "Purple"));
	}

	@Test
	void setValue_withMultiSelectAllOptionsInList_succeeds() {
		CustomFieldValidationRule rule = CustomFieldValidationRule.builder().options("Chess,Football,Music").build();
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"hobbies"))
				.thenReturn(Optional.of(definitionWithRule("hobbies", CustomFieldType.MULTI_SELECT, rule)));

		customFieldValueService.setValue(CustomFieldEntityType.STUDENT, 42L, "hobbies", "Chess,Music");

		verify(entityAttributeService).setAttribute(1L, 42L, "STUDENT", "school-custom-fields", "hobbies",
				"Chess,Music");
	}

	@Test
	void setValue_withMultiSelectOneOptionNotInList_throwsBusinessException() {
		CustomFieldValidationRule rule = CustomFieldValidationRule.builder().options("Chess,Football").build();
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"hobbies"))
				.thenReturn(Optional.of(definitionWithRule("hobbies", CustomFieldType.MULTI_SELECT, rule)));

		assertThrows(BusinessException.class, () -> customFieldValueService.setValue(CustomFieldEntityType.STUDENT,
				42L, "hobbies", "Chess,Dance"));
	}

	@Test
	void getValue_withActiveDefinition_delegatesToEntityAttributeService() {
		when(customFieldDefinitionRepository.findByTenantIdAndEntityTypeAndFieldKey(1L, CustomFieldEntityType.STUDENT,
				"nickname")).thenReturn(Optional.of(activeDefinition("nickname", CustomFieldType.TEXT, false)));
		when(entityAttributeService.getAttribute(1L, 42L, "STUDENT", "school-custom-fields", "nickname"))
				.thenReturn(Optional.of("Ali"));

		Optional<String> value = customFieldValueService.getValue(CustomFieldEntityType.STUDENT, 42L, "nickname");

		assertEquals(Optional.of("Ali"), value);
	}

	@Test
	void getAllValues_mergesDefinitionsWithStoredValues_soUnsetFieldsStillAppear() {
		CustomFieldDefinition nickname = activeDefinition("nickname", CustomFieldType.TEXT, false);
		CustomFieldDefinition bloodGroup = activeDefinition("bloodGroup", CustomFieldType.TEXT, false);
		when(customFieldDefinitionRepository.findAllByTenantIdAndEntityTypeAndActiveTrueOrderByDisplayOrderAsc(1L,
				CustomFieldEntityType.STUDENT)).thenReturn(List.of(nickname, bloodGroup));
		when(entityAttributeService.getAttributes(1L, 42L, "STUDENT", "school-custom-fields"))
				.thenReturn(Map.of("nickname", "Ali"));

		List<CustomFieldValue> values = customFieldValueService.getAllValues(CustomFieldEntityType.STUDENT, 42L);

		assertEquals(2, values.size());
		CustomFieldValue nicknameValue = values.stream().filter(v -> v.fieldKey().equals("nickname")).findFirst()
				.orElseThrow();
		CustomFieldValue bloodGroupValue = values.stream().filter(v -> v.fieldKey().equals("bloodGroup")).findFirst()
				.orElseThrow();
		assertEquals("Ali", nicknameValue.value());
		assertTrue(bloodGroupValue.value() == null);
	}
}
