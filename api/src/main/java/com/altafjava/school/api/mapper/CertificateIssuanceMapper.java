package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CertificateIssuanceResponse;
import com.altafjava.school.domain.certificate.model.CertificateIssuance;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CertificateIssuanceMapper {

	@Mapping(target = "publicId", expression = "java(certificateIssuance.getPublicId().toString())")
	CertificateIssuanceResponse toResponse(CertificateIssuance certificateIssuance);
}
