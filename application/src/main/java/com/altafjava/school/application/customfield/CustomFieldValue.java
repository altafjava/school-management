package com.altafjava.school.application.customfield;

import java.util.List;
import com.altafjava.school.domain.customfield.model.CustomFieldType;

// A defined custom field merged with whatever value (if any) is currently stored for one entity
// instance — value is null when the field is defined but never set, so a caller can tell "unset"
// apart from "doesn't exist". options/displayOrder/displayGroup are rendering hints for a
// frontend form builder (options is only populated for SELECT/MULTI_SELECT fields). visible is
// resolved server-side from the field's visibilityCondition (if any) against the other values for
// this same entity instance — a caller can trust it rather than re-evaluating the condition itself.
public record CustomFieldValue(String fieldKey, String label, CustomFieldType fieldType, boolean required,
		String value, List<String> options, int displayOrder, String displayGroup, boolean visible) {

	public CustomFieldValue {
		options = List.copyOf(options);
	}
}
