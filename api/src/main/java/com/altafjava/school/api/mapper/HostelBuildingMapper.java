package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.HostelBuildingResponse;
import com.altafjava.school.domain.hostel.model.HostelBuilding;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HostelBuildingMapper {

	@Mapping(target = "publicId", expression = "java(building.getPublicId().toString())")
	HostelBuildingResponse toResponse(HostelBuilding building);
}
