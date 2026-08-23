package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAssetRequest(
		@NotBlank @Size(max = 50) String assetCode,
		@NotBlank @Size(max = 150) String name,
		@Size(max = 100) String category,
		LocalDate purchaseDate,
		BigDecimal purchaseCost,
		@Size(max = 150) String location) {
}
