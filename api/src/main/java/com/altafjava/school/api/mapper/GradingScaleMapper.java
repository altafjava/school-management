package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GradingScaleResponse;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;

// No single-entity toResponse(GradingScale) here — the response also needs that scale's
// thresholds, a separate list from a separate table (see GradingScaleThreshold's Javadoc), and a
// MapStruct method combining both as source parameters generates a null-guard that only covers
// "both null", not "scale null alone" — a real NPE path SpotBugs correctly flags. The controller
// builds GradingScaleResponse directly instead; this mapper covers only the genuinely per-element
// entity-to-DTO mapping.
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GradingScaleMapper {

	GradingScaleResponse.Threshold toThreshold(GradingScaleThreshold threshold);
}
