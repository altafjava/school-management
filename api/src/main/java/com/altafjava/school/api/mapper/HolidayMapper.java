package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.HolidayResponse;
import com.altafjava.school.domain.holiday.model.Holiday;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HolidayMapper {

	@Mapping(target = "publicId", expression = "java(holiday.getPublicId().toString())")
	HolidayResponse toResponse(Holiday holiday);
}
