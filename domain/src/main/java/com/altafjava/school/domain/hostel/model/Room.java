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
@Table(name = "hostel_rooms")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Room extends SoftDeletableEntity {

	@Column(name = "hostel_building_id", nullable = false)
	private Long hostelBuildingId;

	@Column(name = "room_number", nullable = false, length = 20)
	private String roomNumber;

	@Column(name = "capacity", nullable = false)
	private int capacity;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Room create(Long hostelBuildingId, String roomNumber, int capacity) {
		return Room.builder()
				.hostelBuildingId(hostelBuildingId)
				.roomNumber(roomNumber)
				.capacity(capacity)
				.active(true)
				.build();
	}

	public void updateDetails(String roomNumber, int capacity) {
		this.roomNumber = roomNumber;
		this.capacity = capacity;
	}

	public void deactivate() {
		this.active = false;
	}
}
