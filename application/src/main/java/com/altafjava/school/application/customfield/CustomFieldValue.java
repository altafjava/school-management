package com.altafjava.school.application.customfield;

import com.altafjava.school.domain.customfield.model.CustomFieldType;

// A defined custom field merged with whatever value (if any) is currently stored for one entity
// instance — value is null when the field is defined but never set, so a caller can tell "unset"
// apart from "doesn't exist".
public record CustomFieldValue(String fieldKey, String label, CustomFieldType fieldType, boolean required,
		String value) {
}
