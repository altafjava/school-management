package com.altafjava.school.domain.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One per teacher per calendar month. The allowance/deduction fields are a snapshot copied from the
 * teacher's {@link SalaryStructure} at generation time (see {@link SalarySnapshot}) — a later
 * revision to that structure must never change an already-generated payslip.
 */
@Entity
@Table(name = "payslips")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Payslip extends SoftDeletableEntity {

	@Column(name = "teacher_id", nullable = false)
	private Long teacherId;

	@Column(name = "pay_year", nullable = false)
	private int payYear;

	@Column(name = "pay_month", nullable = false)
	private int payMonth;

	@Column(name = "basic_pay", nullable = false, precision = 12, scale = 2)
	private BigDecimal basicPay;

	@Column(name = "house_rent_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal houseRentAllowance;

	@Column(name = "transport_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal transportAllowance;

	@Column(name = "other_allowances", nullable = false, precision = 12, scale = 2)
	private BigDecimal otherAllowances;

	@Column(name = "other_deductions", nullable = false, precision = 12, scale = 2)
	private BigDecimal otherDeductions;

	@Column(name = "gross_pay", nullable = false, precision = 12, scale = 2)
	private BigDecimal grossPay;

	@Column(name = "loss_of_pay_days", nullable = false, precision = 5, scale = 1)
	private BigDecimal lossOfPayDays;

	@Column(name = "loss_of_pay_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal lossOfPayAmount;

	@Column(name = "net_pay", nullable = false, precision = 12, scale = 2)
	private BigDecimal netPay;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PayslipStatus status;

	@Column(name = "finalized_at")
	private LocalDateTime finalizedAt;

	@Column(name = "disbursed_at")
	private LocalDateTime disbursedAt;

	public static Payslip generate(Long teacherId, int payYear, int payMonth, SalarySnapshot snapshot,
			PayrollComputation computation) {
		return Payslip.builder()
				.teacherId(teacherId)
				.payYear(payYear)
				.payMonth(payMonth)
				.basicPay(snapshot.basicPay())
				.houseRentAllowance(snapshot.houseRentAllowance())
				.transportAllowance(snapshot.transportAllowance())
				.otherAllowances(snapshot.otherAllowances())
				.otherDeductions(snapshot.otherDeductions())
				.grossPay(computation.grossPay())
				.lossOfPayDays(computation.lossOfPayDays())
				.lossOfPayAmount(computation.lossOfPayAmount())
				.netPay(computation.netPay())
				.status(PayslipStatus.DRAFT)
				.build();
	}

	// Named finalizePayslip rather than finalize() — the latter collides with Object's deprecated
	// finalizer hook.
	public void finalizePayslip() {
		requireStatus(PayslipStatus.DRAFT, "finalize");
		this.status = PayslipStatus.FINALIZED;
		this.finalizedAt = LocalDateTime.now();
	}

	public void markDisbursed() {
		requireStatus(PayslipStatus.FINALIZED, "mark disbursed");
		this.status = PayslipStatus.DISBURSED;
		this.disbursedAt = LocalDateTime.now();
	}

	private void requireStatus(PayslipStatus required, String action) {
		if (this.status != required) {
			throw new BusinessException("Cannot " + action + " a payslip in status " + this.status);
		}
	}
}
