package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ReportCardResponse;
import com.altafjava.school.domain.reportcard.model.ReportCard;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReportCardMapper {

	@Mapping(target = "publicId", expression = "java(reportCard.getPublicId().toString())")
	ReportCardResponse toResponse(ReportCard reportCard);
}
