package com.altafjava.school.domain.customfield.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The tenant-configurable constraints a {@link CustomFieldDefinition}'s values must satisfy —
 * every component is optional and only some apply depending on {@link CustomFieldType}:
 * {@code minValue}/{@code maxValue} for NUMBER, {@code regexPattern} for TEXT, {@code options} for
 * SELECT/MULTI_SELECT. {@link CustomFieldValueService} is what actually enforces these.
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldValidationRule {

	@Column(name = "min_value", precision = 19, scale = 4)
	private BigDecimal minValue;

	@Column(name = "max_value", precision = 19, scale = 4)
	private BigDecimal maxValue;

	@Column(name = "regex_pattern", length = 500)
	private String regexPattern;

	// Comma-separated allowed values — only meaningful for SELECT/MULTI_SELECT; see optionList().
	@Column(name = "options", length = 2000)
	private String options;

	public List<String> optionList() {
		if (options == null || options.isBlank()) {
			return List.of();
		}
		return Arrays.stream(options.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
	}
}
