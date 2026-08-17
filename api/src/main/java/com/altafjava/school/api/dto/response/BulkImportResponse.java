package com.altafjava.school.api.dto.response;

import java.util.List;

public record BulkImportResponse(int totalRows, int successCount, int failureCount, List<RowFailureResponse> failures) {

	public BulkImportResponse {
		failures = List.copyOf(failures);
	}

	public record RowFailureResponse(int rowNumber, String studentCode, String error) {
	}
}
