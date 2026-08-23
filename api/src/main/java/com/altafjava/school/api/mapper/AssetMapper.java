package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AssetResponse;
import com.altafjava.school.domain.inventory.model.Asset;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AssetMapper {

	@Mapping(target = "publicId", expression = "java(asset.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(asset.getStatus().name())")
	AssetResponse toResponse(Asset asset);
}
