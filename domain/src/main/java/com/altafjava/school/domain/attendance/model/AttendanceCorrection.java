package com.altafjava.school.domain.attendance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Append-only audit trail for {@link Attendance#updateStatus} — one row per correction, capturing
 * the before/after status so a disputed attendance record can be answered from history data rather
 * than just {@code updatedBy}/{@code updatedAt} showing a change happened with no record of what
 * it was.
 */
@Entity
@Table(name = "attendance_corrections")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class AttendanceCorrection extends SoftDeletableEntity {

	// FK to attendance.id
	@Column(name = "attendance_id", nullable = false)
	private Long attendanceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "old_status", nullable = false, length = 20)
	private AttendanceStatus oldStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", nullable = false, length = 20)
	private AttendanceStatus newStatus;

	public static AttendanceCorrection record(Long attendanceId, AttendanceStatus oldStatus,
			AttendanceStatus newStatus) {
		return AttendanceCorrection.builder()
				.attendanceId(attendanceId)
				.oldStatus(oldStatus)
				.newStatus(newStatus)
				.build();
	}
}
