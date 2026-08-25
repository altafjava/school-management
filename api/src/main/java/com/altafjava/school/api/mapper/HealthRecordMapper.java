package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.HealthRecordResponse;
import com.altafjava.school.domain.health.model.HealthRecord;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HealthRecordMapper {

	@Mapping(target = "publicId", expression = "java(healthRecord.getPublicId().toString())")
	HealthRecordResponse toResponse(HealthRecord healthRecord);
}
