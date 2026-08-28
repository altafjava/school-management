package com.altafjava.school.domain.customfield.model;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An optional "show this field only if..." rule on a {@link CustomFieldDefinition} — most fields
 * have none ({@link #isPresent()} false). Evaluated by {@code CustomFieldValueService} against the
 * other resolved values for the same entity instance, so a frontend renders exactly what the
 * backend says is visible instead of re-implementing this logic itself.
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldVisibilityCondition {

	@Column(name = "visibility_depends_on_field_key", length = 100)
	private String dependsOnFieldKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility_operator", length = 20)
	private VisibilityOperator operator;

	// For EQUALS/NOT_EQUALS, a single value; for ONE_OF, a comma-separated list — see valueList().
	@Column(name = "visibility_expected_value", length = 500)
	private String expectedValue;

	public boolean isPresent() {
		return dependsOnFieldKey != null && !dependsOnFieldKey.isBlank() && operator != null;
	}

	public List<String> valueList() {
		if (expectedValue == null || expectedValue.isBlank()) {
			return List.of();
		}
		return Arrays.stream(expectedValue.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
	}

	/** {@code actualValue} is the other field's currently-stored value ({@code null} if unset). */
	public boolean isSatisfiedBy(String actualValue) {
		if (!isPresent()) {
			return true;
		}
		return switch (operator) {
			case EQUALS -> !valueList().isEmpty() && valueList().get(0).equals(actualValue);
			case NOT_EQUALS -> valueList().isEmpty() || !valueList().get(0).equals(actualValue);
			case ONE_OF -> actualValue != null && valueList().contains(actualValue);
		};
	}
}
