package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.TermResponse;
import com.altafjava.school.domain.term.model.Term;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TermMapper {

	@Mapping(target = "publicId", expression = "java(term.getPublicId().toString())")
	TermResponse toResponse(Term term);
}
