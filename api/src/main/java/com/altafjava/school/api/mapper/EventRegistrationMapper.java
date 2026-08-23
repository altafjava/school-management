package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.EventRegistrationResponse;
import com.altafjava.school.domain.event.model.EventRegistration;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EventRegistrationMapper {

	@Mapping(target = "publicId", expression = "java(registration.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(registration.getStatus().name())")
	EventRegistrationResponse toResponse(EventRegistration registration);
}
