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
 * A tenant-defined examination/curriculum authority (e.g. a school's own "CBSE", "IB", "State
 * Board" naming) — deliberately not a hardcoded enum, since real schools define their own set.
 */
@Entity
@Table(name = "boards")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Board extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Board create(String name, String code, String description) {
		return Board.builder()
				.name(name)
				.code(code)
				.description(description)
				.active(true)
				.build();
	}

	public void updateDetails(String name, String code, String description) {
		this.name = name;
		this.code = code;
		this.description = description;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
