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
@Table(name = "vehicles")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Vehicle extends SoftDeletableEntity {

	@Column(name = "registration_number", nullable = false, length = 50)
	private String registrationNumber;

	@Column(name = "capacity", nullable = false)
	private int capacity;

	@Column(name = "driver_name", length = 100)
	private String driverName;

	@Column(name = "driver_contact", length = 50)
	private String driverContact;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Vehicle create(String registrationNumber, int capacity, String driverName, String driverContact) {
		return Vehicle.builder()
				.registrationNumber(registrationNumber)
				.capacity(capacity)
				.driverName(driverName)
				.driverContact(driverContact)
				.active(true)
				.build();
	}

	public void updateDetails(int capacity, String driverName, String driverContact) {
		this.capacity = capacity;
		this.driverName = driverName;
		this.driverContact = driverContact;
	}

	public void deactivate() {
		this.active = false;
	}
}
