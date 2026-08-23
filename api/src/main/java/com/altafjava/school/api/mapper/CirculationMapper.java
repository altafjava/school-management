package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CirculationResponse;
import com.altafjava.school.domain.library.model.Circulation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CirculationMapper {

	@Mapping(target = "publicId", expression = "java(circulation.getPublicId().toString())")
	CirculationResponse toResponse(Circulation circulation);
}
