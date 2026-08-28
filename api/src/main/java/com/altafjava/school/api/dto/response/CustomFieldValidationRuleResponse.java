package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CustomFieldValidationRuleResponse(
		BigDecimal minValue,
		BigDecimal maxValue,
		String regexPattern,
		List<String> options) {

	public CustomFieldValidationRuleResponse {
		options = List.copyOf(options);
	}
}
