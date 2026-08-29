package com.altafjava.school.domain.exam.model;

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
 * Tenant-defined exam category (e.g. Unit Test, Midterm, Final, or a board-specific scheme like
 * "Formative"/"Summative") — schools and boards vary widely here, so this is a runtime catalog
 * rather than a fixed enum, mirroring {@code LeaveType}/{@code PayComponentDefinition}.
 */
@Entity
@Table(name = "exam_type_definitions")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ExamTypeDefinition extends SoftDeletableEntity {

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static ExamTypeDefinition create(String code, String name, int displayOrder) {
		return ExamTypeDefinition.builder()
				.code(code)
				.name(name)
				.displayOrder(displayOrder)
				.active(true)
				.build();
	}

	public void update(String name, boolean active, int displayOrder) {
		this.name = name;
		this.active = active;
		this.displayOrder = displayOrder;
	}
}
