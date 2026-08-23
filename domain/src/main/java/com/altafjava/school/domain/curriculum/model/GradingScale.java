package com.altafjava.school.domain.curriculum.model;

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
 * A tenant-defined, named grading scale — real schools need more than one (e.g. a percentage-based
 * scale for a CBSE-affiliated curriculum, a 1-7 point scale for an IB one), each with its own
 * thresholds ({@link GradingScaleThreshold}, a separate table — see that class for why). Exactly
 * one scale per tenant may be marked {@code isDefault}, enforced at the service layer.
 */
@Entity
@Table(name = "grading_scales")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class GradingScale extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static GradingScale create(String name, boolean isDefault) {
		return GradingScale.builder()
				.name(name)
				.isDefault(isDefault)
				.active(true)
				.build();
	}

	public void rename(String name) {
		this.name = name;
	}

	public void markAsDefault() {
		this.isDefault = true;
	}

	public void unmarkAsDefault() {
		this.isDefault = false;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
