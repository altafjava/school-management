package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.TransportAssignmentResponse;
import com.altafjava.school.domain.transport.model.TransportAssignment;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransportAssignmentMapper {

	@Mapping(target = "publicId", expression = "java(assignment.getPublicId().toString())")
	TransportAssignmentResponse toResponse(TransportAssignment assignment);
}
