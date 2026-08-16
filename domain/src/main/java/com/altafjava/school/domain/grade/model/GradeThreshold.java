package com.altafjava.school.domain.grade.model;

import java.math.BigDecimal;

// One step of a grading scale: letter is awarded when a percentage is at least minPercentage.
public record GradeThreshold(String letter, BigDecimal minPercentage) {
}
