package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeeStructureRevisionResponse;
import com.altafjava.school.domain.fee.model.FeeStructureRevision;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeStructureRevisionMapper {

	@Mapping(target = "publicId", expression = "java(revision.getPublicId().toString())")
	@Mapping(target = "revisedBy", source = "createdBy")
	@Mapping(target = "revisedAt", source = "createdAt")
	FeeStructureRevisionResponse toResponse(FeeStructureRevision revision);
}
