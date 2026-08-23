package com.altafjava.school.application.service;

import java.math.BigDecimal;

// A single letter/minPercentage/points step supplied when creating or replacing a GradingScale's
// thresholds — not itself persisted, GradingScaleService turns these into GradingScaleThreshold rows.
public record GradingScaleThresholdInput(String letter, BigDecimal minPercentage, BigDecimal points) {
}
