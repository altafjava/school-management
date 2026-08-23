package com.altafjava.school.domain.leave.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Tenant-defined leave category (e.g. Sick, Casual, Earned) — schools vary widely here, so this is
 * a runtime catalog rather than a hardcoded enum, mirroring {@code Department}.
 */
@Entity
@Table(name = "leave_types")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LeaveType extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "default_annual_days", nullable = false, precision = 5, scale = 1)
	private BigDecimal defaultAnnualDays;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static LeaveType create(String name, BigDecimal defaultAnnualDays) {
		return LeaveType.builder()
				.name(name)
				.defaultAnnualDays(defaultAnnualDays)
				.active(true)
				.build();
	}

	public void updateDetails(String name, BigDecimal defaultAnnualDays) {
		this.name = name;
		this.defaultAnnualDays = defaultAnnualDays;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
