package com.altafjava.school.api.dto.response;

import java.util.List;
import com.altafjava.school.domain.customfield.model.VisibilityOperator;

public record CustomFieldVisibilityConditionResponse(
		String dependsOnFieldKey,
		VisibilityOperator operator,
		List<String> expectedValues) {

	public CustomFieldVisibilityConditionResponse {
		expectedValues = List.copyOf(expectedValues);
	}
}
