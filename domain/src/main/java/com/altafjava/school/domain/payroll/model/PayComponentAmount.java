package com.altafjava.school.domain.payroll.model;

import java.math.BigDecimal;

/**
 * One line item of a {@link SalaryStructure}/{@link Payslip} — the component's own name/type are
 * copied in at the time the amount is set (not just a component-definition id) so a later rename or
 * deactivation of the tenant's {@link PayComponentDefinition} catalog entry never changes how an
 * already-recorded structure or payslip reads.
 */
public record PayComponentAmount(String code, String name, PayComponentType type, BigDecimal amount) {
}
