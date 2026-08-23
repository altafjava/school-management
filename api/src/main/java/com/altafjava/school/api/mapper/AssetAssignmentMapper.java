package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AssetAssignmentResponse;
import com.altafjava.school.domain.inventory.model.AssetAssignment;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AssetAssignmentMapper {

	@Mapping(target = "publicId", expression = "java(assignment.getPublicId().toString())")
	@Mapping(target = "assignedToType", expression = "java(assignment.getAssignedToType().name())")
	AssetAssignmentResponse toResponse(AssetAssignment assignment);
}
