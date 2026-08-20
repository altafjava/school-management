package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.LessonResponse;
import com.altafjava.school.domain.lms.model.Lesson;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LessonMapper {

	@Mapping(target = "publicId", expression = "java(lesson.getPublicId().toString())")
	LessonResponse toResponse(Lesson lesson);
}
