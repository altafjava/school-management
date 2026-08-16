package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record GradingScaleResponse(List<Threshold> thresholds) {

	public GradingScaleResponse {
		thresholds = List.copyOf(thresholds);
	}

	public record Threshold(String letter, BigDecimal minPercentage) {
	}
}
