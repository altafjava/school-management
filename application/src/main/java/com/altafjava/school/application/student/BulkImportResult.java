package com.altafjava.school.application.student;

import java.util.List;

public record BulkImportResult(int totalRows, int successCount, int failureCount, List<RowFailure> failures) {

	public BulkImportResult {
		failures = List.copyOf(failures);
	}

	public record RowFailure(int rowNumber, String studentCode, String error) {
	}
}
