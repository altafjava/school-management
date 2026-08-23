package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.VehicleResponse;
import com.altafjava.school.domain.transport.model.Vehicle;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VehicleMapper {

	@Mapping(target = "publicId", expression = "java(vehicle.getPublicId().toString())")
	VehicleResponse toResponse(Vehicle vehicle);
}
