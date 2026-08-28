package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Every component is optional — which ones apply depends on the owning definition's fieldType:
// minValue/maxValue for NUMBER, regexPattern for TEXT, options for SELECT/MULTI_SELECT.
public record CustomFieldValidationRuleRequest(
		BigDecimal minValue,
		BigDecimal maxValue,
		@Size(max = 500) String regexPattern,
		List<@NotBlank @Size(max = 200) String> options) {

	public CustomFieldValidationRuleRequest {
		options = options == null ? null : List.copyOf(options);
	}
}
