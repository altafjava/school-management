package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.altafjava.platform.application.extension.EntityAttributeService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.customfield.CustomFieldValue;
import com.altafjava.school.domain.customfield.model.CustomFieldDefinition;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.customfield.model.CustomFieldType;
import com.altafjava.school.domain.customfield.model.CustomFieldValidationRule;
import com.altafjava.school.domain.customfield.repository.CustomFieldDefinitionRepository;

/**
 * Wraps platform's generic {@link EntityAttributeService} (Pattern B of the entity-extension
 * contract — a sparse tenant-scoped key/value store, {@code entity_extended_attributes}) under a
 * fixed {@code domain} namespace, so school-saas's custom fields never collide with any other
 * domain project's use of the same generic mechanism.
 *
 * <p>
 * Every read/write here is validated against an active {@link CustomFieldDefinition} first — this
 * service is the only allowed writer of school custom-field values specifically so that
 * validation can never be bypassed by calling {@link EntityAttributeService} directly with this
 * namespace. Unknown/inactive field keys and type-mismatched values fail fast with a
 * {@link BusinessException} (400) rather than being silently stored as opaque strings.
 */
@Service
public class CustomFieldValueService {

	// Namespaces every school-saas custom-field value in platform's shared extension table,
	// isolating it from any other domain/feature that might also use EntityAttributeService.
	static final String DOMAIN = "school-custom-fields";

	private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
	private final EntityAttributeService entityAttributeService;

	public CustomFieldValueService(CustomFieldDefinitionRepository customFieldDefinitionRepository,
			EntityAttributeService entityAttributeService) {
		this.customFieldDefinitionRepository = customFieldDefinitionRepository;
		this.entityAttributeService = entityAttributeService;
	}

	public void setValue(CustomFieldEntityType entityType, Long entityId, String fieldKey, String rawValue) {
		Long tenantId = TenantContext.getCurrentTenantId();
		CustomFieldDefinition definition = requireActiveDefinition(tenantId, entityType, fieldKey);
		if (definition.isRequired() && (rawValue == null || rawValue.isBlank())) {
			throw new BusinessException("Custom field '" + fieldKey + "' is required and cannot be cleared");
		}
		if (rawValue != null && !rawValue.isBlank()) {
			validateValue(definition, rawValue);
		}
		entityAttributeService.setAttribute(tenantId, entityId, entityType.name(), DOMAIN, fieldKey, rawValue);
	}

	public Optional<String> getValue(CustomFieldEntityType entityType, Long entityId, String fieldKey) {
		Long tenantId = TenantContext.getCurrentTenantId();
		requireActiveDefinition(tenantId, entityType, fieldKey);
		return entityAttributeService.getAttribute(tenantId, entityId, entityType.name(), DOMAIN, fieldKey);
	}

	/**
	 * Merges every active definition for {@code entityType} with whatever values are actually
	 * stored for {@code entityId} — a field with no stored value still appears, with a {@code null}
	 * value, so a caller can distinguish "defined but unset" from "not a defined field at all".
	 */
	public List<CustomFieldValue> getAllValues(CustomFieldEntityType entityType, Long entityId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		List<CustomFieldDefinition> definitions = customFieldDefinitionRepository
				.findAllByTenantIdAndEntityTypeAndActiveTrueOrderByDisplayOrderAsc(tenantId, entityType);
		var storedValues = entityAttributeService.getAttributes(tenantId, entityId, entityType.name(), DOMAIN);
		return definitions.stream()
				.map(definition -> new CustomFieldValue(definition.getFieldKey(), definition.getLabel(),
						definition.getFieldType(), definition.isRequired(),
						storedValues.get(definition.getFieldKey()), definition.getValidationRule().optionList(),
						definition.getDisplayOrder(), definition.getDisplayGroup()))
				.toList();
	}

	private CustomFieldDefinition requireActiveDefinition(Long tenantId, CustomFieldEntityType entityType,
			String fieldKey) {
		CustomFieldDefinition definition = customFieldDefinitionRepository
				.findByTenantIdAndEntityTypeAndFieldKey(tenantId, entityType, fieldKey)
				.orElseThrow(() -> new BusinessException(
						"Unknown custom field for " + entityType + ": " + fieldKey));
		if (!definition.isActive()) {
			throw new BusinessException("Custom field is not active: " + fieldKey);
		}
		return definition;
	}

	private void validateValue(CustomFieldDefinition definition, String rawValue) {
		CustomFieldType fieldType = definition.getFieldType();
		CustomFieldValidationRule rule = definition.getValidationRule();
		try {
			switch (fieldType) {
				case NUMBER -> validateNumber(definition, rule, rawValue);
				case DATE -> LocalDate.parse(rawValue);
				case BOOLEAN -> {
					if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
						throw new BusinessException(
								"Custom field '" + definition.getFieldKey() + "' expects a BOOLEAN (true/false)"
										+ " value, got: " + rawValue);
					}
				}
				case TEXT -> validateText(definition, rule, rawValue);
				case SELECT -> validateOption(definition, rule, rawValue);
				case MULTI_SELECT -> Arrays.stream(rawValue.split(","))
						.map(String::trim)
						.filter(value -> !value.isEmpty())
						.forEach(value -> validateOption(definition, rule, value));
			}
		} catch (NumberFormatException e) {
			throw new BusinessException(
					"Custom field '" + definition.getFieldKey() + "' expects a NUMBER value, got: " + rawValue);
		} catch (DateTimeParseException e) {
			throw new BusinessException("Custom field '" + definition.getFieldKey()
					+ "' expects an ISO-8601 DATE value (yyyy-MM-dd), got: " + rawValue);
		}
	}

	private void validateNumber(CustomFieldDefinition definition, CustomFieldValidationRule rule, String rawValue) {
		BigDecimal value = new BigDecimal(rawValue);
		if (rule.getMinValue() != null && value.compareTo(rule.getMinValue()) < 0) {
			throw new BusinessException("Custom field '" + definition.getFieldKey() + "' must be at least "
					+ rule.getMinValue() + ", got: " + rawValue);
		}
		if (rule.getMaxValue() != null && value.compareTo(rule.getMaxValue()) > 0) {
			throw new BusinessException("Custom field '" + definition.getFieldKey() + "' must be at most "
					+ rule.getMaxValue() + ", got: " + rawValue);
		}
	}

	private void validateText(CustomFieldDefinition definition, CustomFieldValidationRule rule, String rawValue) {
		if (rule.getRegexPattern() != null && !rawValue.matches(rule.getRegexPattern())) {
			throw new BusinessException("Custom field '" + definition.getFieldKey()
					+ "' does not match the required pattern: " + rawValue);
		}
	}

	private void validateOption(CustomFieldDefinition definition, CustomFieldValidationRule rule, String rawValue) {
		if (!rule.optionList().contains(rawValue)) {
			throw new BusinessException("Custom field '" + definition.getFieldKey()
					+ "' does not allow value '" + rawValue + "' — allowed: " + rule.optionList());
		}
	}
}
