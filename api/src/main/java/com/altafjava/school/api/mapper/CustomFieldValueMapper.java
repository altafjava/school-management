package com.altafjava.school.api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CustomFieldValueResponse;
import com.altafjava.school.application.customfield.CustomFieldValue;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomFieldValueMapper {

	CustomFieldValueResponse toResponse(CustomFieldValue customFieldValue);

	List<CustomFieldValueResponse> toResponseList(List<CustomFieldValue> customFieldValues);
}
