package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.SalaryStructureResponse;
import com.altafjava.school.domain.payroll.model.SalaryStructure;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SalaryStructureMapper {

	@Mapping(target = "publicId", expression = "java(salaryStructure.getPublicId().toString())")
	SalaryStructureResponse toResponse(SalaryStructure salaryStructure);
}
