package com.altafjava.school.domain.leave.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

@Entity
@Table(name = "leave_requests")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LeaveRequest extends SoftDeletableEntity {

	@Column(name = "teacher_id", nullable = false)
	private Long teacherId;

	@Column(name = "leave_type_id", nullable = false)
	private Long leaveTypeId;

	// Captured at submit time rather than re-resolved at approval time, so an academic-year
	// rollover between submission and approval cannot silently point the deduction at the wrong
	// year's balance.
	@Column(name = "academic_year_id", nullable = false)
	private Long academicYearId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "days_requested", nullable = false, precision = 5, scale = 1)
	private BigDecimal daysRequested;

	@Column(name = "reason", length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private LeaveRequestStatus status;

	@Column(name = "approved_by_user_id")
	private Long approvedByUserId;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	public static LeaveRequest submit(Long teacherId, Long leaveTypeId, Long academicYearId, LocalDate startDate,
			LocalDate endDate, String reason) {
		if (endDate.isBefore(startDate)) {
			throw new BusinessException("Leave end date cannot be before the start date");
		}
		long inclusiveDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		return LeaveRequest.builder()
				.teacherId(teacherId)
				.leaveTypeId(leaveTypeId)
				.academicYearId(academicYearId)
				.startDate(startDate)
				.endDate(endDate)
				.daysRequested(BigDecimal.valueOf(inclusiveDays))
				.reason(reason)
				.status(LeaveRequestStatus.PENDING)
				.build();
	}

	public void approve(Long approvedByUserId) {
		requireStatus(LeaveRequestStatus.PENDING, "approve");
		this.status = LeaveRequestStatus.APPROVED;
		this.approvedByUserId = approvedByUserId;
		this.approvedAt = LocalDateTime.now();
	}

	public void reject(Long rejectedByUserId, String rejectionReason) {
		requireStatus(LeaveRequestStatus.PENDING, "reject");
		this.status = LeaveRequestStatus.REJECTED;
		this.approvedByUserId = rejectedByUserId;
		this.approvedAt = LocalDateTime.now();
		this.rejectionReason = rejectionReason;
	}

	public void cancel() {
		if (this.status == LeaveRequestStatus.REJECTED || this.status == LeaveRequestStatus.CANCELLED) {
			throw new BusinessException("Cannot cancel a leave request that is already " + this.status);
		}
		if (this.startDate.isBefore(LocalDate.now())) {
			throw new BusinessException("Cannot cancel a leave request that has already started");
		}
		this.status = LeaveRequestStatus.CANCELLED;
	}

	private void requireStatus(LeaveRequestStatus required, String action) {
		if (this.status != required) {
			throw new BusinessException("Cannot " + action + " a leave request in status " + this.status);
		}
	}
}
