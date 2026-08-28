package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GuardianResponse;
import com.altafjava.school.domain.guardian.model.Guardian;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = AddressMapper.class)
public interface GuardianMapper {

	@Mapping(target = "publicId", expression = "java(guardian.getPublicId().toString())")
	GuardianResponse toResponse(Guardian guardian);
}
