package com.altafjava.school.application.customfield;

import java.util.List;

// One ordered section of an entity's custom fields — groupName is the raw displayGroup string
// ("" for fields with no group), ordered by the lowest displayGroupOrder among its fields.
public record FieldGroup(String groupName, int order, List<CustomFieldValue> fields) {

	public FieldGroup {
		fields = List.copyOf(fields);
	}
}
