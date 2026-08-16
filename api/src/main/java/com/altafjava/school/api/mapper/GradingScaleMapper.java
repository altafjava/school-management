package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GradingScaleResponse;
import com.altafjava.school.domain.grade.model.GradingScale;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GradingScaleMapper {

	default GradingScaleResponse toResponse(GradingScale gradingScale) {
		return new GradingScaleResponse(gradingScale.thresholds().stream()
				.map(threshold -> new GradingScaleResponse.Threshold(threshold.letter(), threshold.minPercentage()))
				.toList());
	}
}
