package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.BoardResponse;
import com.altafjava.school.domain.curriculum.model.Board;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BoardMapper {

	@Mapping(target = "publicId", expression = "java(board.getPublicId().toString())")
	BoardResponse toResponse(Board board);
}
