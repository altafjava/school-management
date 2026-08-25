package com.altafjava.school.domain.hostel.model;

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
@Table(name = "hostel_buildings")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class HostelBuilding extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "address", length = 500)
	private String address;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static HostelBuilding create(String name, String address) {
		return HostelBuilding.builder()
				.name(name)
				.address(address)
				.active(true)
				.build();
	}

	public void updateDetails(String name, String address) {
		this.name = name;
		this.address = address;
	}

	public void deactivate() {
		this.active = false;
	}
}
