package com.altafjava.school.api.dto.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.constraints.NotEmpty;

// Map of fieldKey -> raw string value. A null value clears the field (rejected by
// CustomFieldValueService if the field is required). Each key is validated against an active
// CustomFieldDefinition by the service layer, not here — this DTO only enforces "at least one
// entry was submitted". Defensively copied (like UpdateGradingScaleThresholdsRequest's
// List.copyOf) rather than using Map.copyOf, since Map.copyOf rejects null values and this DTO
// deliberately allows one (to mean "clear this field").
public record SetCustomFieldValuesRequest(@NotEmpty Map<String, String> values) {

	public SetCustomFieldValuesRequest {
		values = values == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}
}
