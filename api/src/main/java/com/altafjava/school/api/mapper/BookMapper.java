package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.BookResponse;
import com.altafjava.school.domain.library.model.Book;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookMapper {

	@Mapping(target = "publicId", expression = "java(book.getPublicId().toString())")
	BookResponse toResponse(Book book);
}
