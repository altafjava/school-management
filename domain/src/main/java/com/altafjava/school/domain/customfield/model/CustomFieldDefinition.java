package com.altafjava.school.domain.customfield.model;

import jakarta.persistence.Column;
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

	public static CustomFieldDefinition create(CustomFieldEntityType entityType, String fieldKey, String label,
			CustomFieldType fieldType, boolean required) {
		return CustomFieldDefinition.builder()
				.entityType(entityType)
				.fieldKey(fieldKey)
				.label(label)
				.fieldType(fieldType)
				.required(required)
				.active(true)
				.build();
	}

	public void updateDetails(String label, CustomFieldType fieldType, boolean required) {
		this.label = label;
		this.fieldType = fieldType;
		this.required = required;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
