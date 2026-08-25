package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CertificateTemplateResponse;
import com.altafjava.school.domain.certificate.model.CertificateTemplate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CertificateTemplateMapper {

	@Mapping(target = "publicId", expression = "java(certificateTemplate.getPublicId().toString())")
	CertificateTemplateResponse toResponse(CertificateTemplate certificateTemplate);
}
