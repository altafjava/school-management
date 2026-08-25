package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CertificateVerificationResponse;
import com.altafjava.school.application.certificate.CertificateVerificationResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CertificateVerificationMapper {

	CertificateVerificationResponse toResponse(CertificateVerificationResult result);
}
