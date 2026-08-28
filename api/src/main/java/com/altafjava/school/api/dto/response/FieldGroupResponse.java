package com.altafjava.school.api.dto.response;

import java.util.List;

public record FieldGroupResponse(String groupName, int order, List<CustomFieldValueResponse> fields) {

	public FieldGroupResponse {
		fields = List.copyOf(fields);
	}
}
