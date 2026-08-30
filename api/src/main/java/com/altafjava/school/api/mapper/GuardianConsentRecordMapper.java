package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GuardianConsentRecordResponse;
import com.altafjava.school.domain.guardian.model.GuardianConsentRecord;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GuardianConsentRecordMapper {

	@Mapping(target = "id", expression = "java(consentRecord.getPublicId().toString())")
	GuardianConsentRecordResponse toResponse(GuardianConsentRecord consentRecord);
}
