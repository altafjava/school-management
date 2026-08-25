package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.VisitorLogResponse;
import com.altafjava.school.domain.visitor.model.VisitorLog;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VisitorLogMapper {

	@Mapping(target = "publicId", expression = "java(visitorLog.getPublicId().toString())")
	VisitorLogResponse toResponse(VisitorLog visitorLog);
}
