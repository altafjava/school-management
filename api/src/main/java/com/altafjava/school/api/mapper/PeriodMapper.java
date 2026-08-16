package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.PeriodResponse;
import com.altafjava.school.domain.timetable.model.Period;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PeriodMapper {

	@Mapping(target = "publicId", expression = "java(period.getPublicId().toString())")
	PeriodResponse toResponse(Period period);
}
