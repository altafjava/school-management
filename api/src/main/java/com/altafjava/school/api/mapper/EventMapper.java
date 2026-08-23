package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.EventResponse;
import com.altafjava.school.domain.event.model.Event;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EventMapper {

	@Mapping(target = "publicId", expression = "java(event.getPublicId().toString())")
	EventResponse toResponse(Event event);
}
