package com.altafjava.school.domain.leave.model;

import java.math.BigDecimal;
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

@Entity
@Table(name = "leave_balances")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LeaveBalance extends SoftDeletableEntity {

	@Column(name = "teacher_id", nullable = false)
	private Long teacherId;

	@Column(name = "leave_type_id", nullable = false)
	private Long leaveTypeId;

	@Column(name = "academic_year_id", nullable = false)
	private Long academicYearId;

	@Column(name = "allocated_days", nullable = false, precision = 5, scale = 1)
	private BigDecimal allocatedDays;

	@Column(name = "used_days", nullable = false, precision = 5, scale = 1)
	private BigDecimal usedDays;

	public static LeaveBalance allocate(Long teacherId, Long leaveTypeId, Long academicYearId,
			BigDecimal allocatedDays) {
		return LeaveBalance.builder()
				.teacherId(teacherId)
				.leaveTypeId(leaveTypeId)
				.academicYearId(academicYearId)
				.allocatedDays(allocatedDays)
				.usedDays(BigDecimal.ZERO)
				.build();
	}

	public BigDecimal remainingDays() {
		return allocatedDays.subtract(usedDays);
	}

	public void deduct(BigDecimal days) {
		if (days.compareTo(remainingDays()) > 0) {
			throw new BusinessException("Insufficient leave balance: requested " + days + ", remaining "
					+ remainingDays());
		}
		this.usedDays = this.usedDays.add(days);
	}

	public void credit(BigDecimal days) {
		BigDecimal reversed = this.usedDays.subtract(days);
		this.usedDays = reversed.max(BigDecimal.ZERO);
	}
}
