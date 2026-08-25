package com.altafjava.school.domain.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A teacher's compensation terms as of {@link #effectiveFrom}. At most one structure is
 * {@link #active} per teacher at a time — {@code SalaryStructureService} deactivates the previous
 * one when a new one is created, mirroring how {@code Term}/{@code AcademicYear} flip {@code current}.
 */
@Entity
@Table(name = "salary_structures")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SalaryStructure extends SoftDeletableEntity {

	@Column(name = "teacher_id", nullable = false)
	private Long teacherId;

	@Column(name = "basic_pay", nullable = false, precision = 12, scale = 2)
	private BigDecimal basicPay;

	@Column(name = "house_rent_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal houseRentAllowance;

	@Column(name = "transport_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal transportAllowance;

	@Column(name = "other_allowances", nullable = false, precision = 12, scale = 2)
	private BigDecimal otherAllowances;

	// Free-text-adjacent by design — e.g. a manually computed tax withholding a school wants on
	// record. This system does not compute statutory deductions; see docs/architecture-analysis.
	@Column(name = "other_deductions", nullable = false, precision = 12, scale = 2)
	private BigDecimal otherDeductions;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static SalaryStructure create(Long teacherId, BigDecimal basicPay, BigDecimal houseRentAllowance,
			BigDecimal transportAllowance, BigDecimal otherAllowances, BigDecimal otherDeductions,
			LocalDate effectiveFrom) {
		if (basicPay == null || basicPay.signum() <= 0) {
			throw new BusinessException("Basic pay must be greater than zero");
		}
		return SalaryStructure.builder()
				.teacherId(teacherId)
				.basicPay(basicPay)
				.houseRentAllowance(houseRentAllowance)
				.transportAllowance(transportAllowance)
				.otherAllowances(otherAllowances)
				.otherDeductions(otherDeductions)
				.effectiveFrom(effectiveFrom)
				.active(true)
				.build();
	}

	public void deactivate() {
		this.active = false;
	}

	public BigDecimal grossPay() {
		return basicPay.add(houseRentAllowance).add(transportAllowance).add(otherAllowances);
	}

	public SalarySnapshot toSnapshot() {
		return new SalarySnapshot(basicPay, houseRentAllowance, transportAllowance, otherAllowances, otherDeductions);
	}
}
