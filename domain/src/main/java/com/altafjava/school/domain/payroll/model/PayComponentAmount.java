package com.altafjava.school.domain.payroll.model;

import java.math.BigDecimal;

/**
 * One line item of a {@link SalaryStructure}/{@link Payslip} — name/type are copied in at set time
 * so a later catalog rename never changes how an already-recorded structure reads.
 */
public record PayComponentAmount(String code, String name, PayComponentType type, BigDecimal amount) {
}
