package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.BulkImportResponse;
import com.altafjava.school.api.dto.response.BulkImportResponse.RowFailureResponse;
import com.altafjava.school.application.student.BulkImportResult;
import com.altafjava.school.application.student.BulkImportResult.RowFailure;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BulkImportMapper {

	BulkImportResponse toResponse(BulkImportResult result);

	RowFailureResponse toResponse(RowFailure failure);
}
