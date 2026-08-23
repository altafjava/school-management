package com.altafjava.school.domain.transport.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "routes")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Route extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Route create(String name, String code, String description) {
		return Route.builder()
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

	public void deactivate() {
		this.active = false;
	}
}
