package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetResponse(
		String publicId,
		String assetCode,
		String name,
		String category,
		LocalDate purchaseDate,
		BigDecimal purchaseCost,
		String location,
		String status) {
}
