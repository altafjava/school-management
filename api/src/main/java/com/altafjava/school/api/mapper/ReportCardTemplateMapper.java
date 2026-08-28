package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ReportCardTemplateResponse;
import com.altafjava.school.domain.reportcard.model.ReportCardTemplate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReportCardTemplateMapper {

	ReportCardTemplateResponse toResponse(ReportCardTemplate template);
}
