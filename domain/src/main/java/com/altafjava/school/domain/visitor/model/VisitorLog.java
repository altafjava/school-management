package com.altafjava.school.domain.visitor.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * {@code hostTeacherId} references {@code Teacher} (school-saas's staff/employee entity — the same
 * one payroll's {@code SalaryStructure}/{@code Payslip} attach to for every staff role, not only
 * classroom teachers) rather than a raw platform {@code User} id: every other cross-entity
 * reference in this codebase resolves through a repository via a client-supplied public id, and
 * {@code Teacher} is the one staff-facing entity in school-saas's own domain boundary with that
 * lookup already available. A raw platform user id would be the one field in this API surface
 * bypassing that convention.
 */
@Entity
@Table(name = "visitor_logs")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class VisitorLog extends SoftDeletableEntity {

	@Pii
	@Column(name = "visitor_name", nullable = false, length = 150)
	private String visitorName;

	@Pii
	@Column(name = "visitor_phone", length = 50)
	private String visitorPhone;

	@Column(name = "purpose", nullable = false, length = 500)
	private String purpose;

	@Column(name = "host_teacher_id", nullable = false)
	private Long hostTeacherId;

	@Column(name = "check_in_at", nullable = false)
	private LocalDateTime checkInAt;

	@Column(name = "check_out_at")
	private LocalDateTime checkOutAt;

	public static VisitorLog checkIn(String visitorName, String visitorPhone, String purpose, Long hostTeacherId,
			LocalDateTime checkInAt) {
		return VisitorLog.builder()
				.visitorName(visitorName)
				.visitorPhone(visitorPhone)
				.purpose(purpose)
				.hostTeacherId(hostTeacherId)
				.checkInAt(checkInAt)
				.build();
	}

	public void checkOut(LocalDateTime checkOutAt) {
		if (this.checkOutAt != null) {
			throw new BusinessException("Visitor already checked out at " + this.checkOutAt);
		}
		if (checkOutAt.isBefore(this.checkInAt)) {
			throw new BusinessException("Check-out time cannot be before check-in time");
		}
		this.checkOutAt = checkOutAt;
	}
}
