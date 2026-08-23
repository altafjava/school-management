package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.RouteStopResponse;
import com.altafjava.school.domain.transport.model.RouteStop;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RouteStopMapper {

	@Mapping(target = "publicId", expression = "java(routeStop.getPublicId().toString())")
	RouteStopResponse toResponse(RouteStop routeStop);
}
