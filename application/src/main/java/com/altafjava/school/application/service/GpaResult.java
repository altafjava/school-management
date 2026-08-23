package com.altafjava.school.application.service;

import java.math.BigDecimal;

// gpa is null when gradeCount is 0 — distinguishes "no grades yet" from a real 0.00 average.
public record GpaResult(BigDecimal gpa, int gradeCount) {
}
