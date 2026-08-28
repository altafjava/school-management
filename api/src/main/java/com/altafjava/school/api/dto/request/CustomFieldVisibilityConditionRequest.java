package com.altafjava.school.api.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.customfield.model.VisibilityOperator;

// Optional — omit entirely for a field with no "show only if..." rule. expectedValues takes one
// entry for EQUALS/NOT_EQUALS, one or more for ONE_OF.
public record CustomFieldVisibilityConditionRequest(
		@Size(max = 100) String dependsOnFieldKey,
		VisibilityOperator operator,
		List<@NotBlank @Size(max = 200) String> expectedValues) {

	public CustomFieldVisibilityConditionRequest {
		expectedValues = expectedValues == null ? null : List.copyOf(expectedValues);
	}
}
