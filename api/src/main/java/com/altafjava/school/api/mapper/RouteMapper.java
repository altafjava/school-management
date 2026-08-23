package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.RouteResponse;
import com.altafjava.school.domain.transport.model.Route;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RouteMapper {

	@Mapping(target = "publicId", expression = "java(route.getPublicId().toString())")
	RouteResponse toResponse(Route route);
}
