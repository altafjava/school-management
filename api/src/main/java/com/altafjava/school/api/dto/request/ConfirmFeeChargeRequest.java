package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

// studentPublicId/feeStructurePublicId are the same values the caller used to create the charge —
// PaymentGatewayProvider#getChargeStatus intentionally carries no metadata to recover them from.
public record ConfirmFeeChargeRequest(
		@NotBlank String studentPublicId,
		@NotBlank String feeStructurePublicId) {
}
