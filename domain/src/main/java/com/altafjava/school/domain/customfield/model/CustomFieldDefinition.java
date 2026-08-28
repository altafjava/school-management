package com.altafjava.school.domain.customfield.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Tenant-admin-defined schema for one dynamic/custom field on a platform entity (e.g. Student's
 * "Blood Group"). This is the only new school-saas schema Part 2 introduces — the field *values*
 * themselves are never stored here; they live in platform's existing generic
 * {@code entity_extended_attributes} table via {@code EntityAttributeService}, keyed by
 * ({@code entityType}, {@code fieldKey}) against the definition a tenant admin declares here. See
 * {@code CustomFieldValueService}.
 */
@Entity
@Table(name = "custom_field_definitions")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CustomFieldDefinition extends SoftDeletableEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "entity_type", nullable = false, length = 30)
	private CustomFieldEntityType entityType;

	// Machine key used to store/look up values, e.g. "bloodGroup" — not the display label.
	@Column(name = "field_key", nullable = false, length = 100)
	private String fieldKey;

	@Column(name = "label", nullable = false, length = 200)
	private String label;

	@Enumerated(EnumType.STRING)
	@Column(name = "field_type", nullable = false, length = 20)
	private CustomFieldType fieldType;

	@Column(name = "required", nullable = false)
	private boolean required;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Embedded
	private CustomFieldValidationRule validationRule;

	// Rendering hints for a frontend form builder — never interpreted by this backend itself.
	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "display_group", length = 100)
	private String displayGroup;

	public static CustomFieldDefinition create(CustomFieldEntityType entityType, String fieldKey, String label,
			CustomFieldType fieldType, boolean required) {
		return CustomFieldDefinition.builder()
				.entityType(entityType)
				.fieldKey(fieldKey)
				.label(label)
				.fieldType(fieldType)
				.required(required)
				.active(true)
				.validationRule(CustomFieldValidationRule.builder().build())
				.build();
	}

	public void updateDetails(String label, CustomFieldType fieldType, boolean required) {
		this.label = label;
		this.fieldType = fieldType;
		this.required = required;
	}

	public void updateValidationRule(CustomFieldValidationRule validationRule) {
		this.validationRule = validationRule != null ? validationRule : CustomFieldValidationRule.builder().build();
	}

	// Hibernate collapses an @Embedded value back to null on load when every one of its mapped
	// columns is null (e.g. a definition created before validationRule existed, or one that never
	// set any rule) — callers must never see that null, only an empty rule.
	public CustomFieldValidationRule getValidationRule() {
		return validationRule != null ? validationRule : CustomFieldValidationRule.builder().build();
	}

	public void reorder(int displayOrder, String displayGroup) {
		this.displayOrder = displayOrder;
		this.displayGroup = displayGroup;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
