package com.altafjava.school.domain.event.model;

import java.time.LocalDateTime;
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
@Table(name = "events")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Event extends SoftDeletableEntity {

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "event_date", nullable = false)
	private LocalDateTime eventDate;

	@Column(name = "location", length = 200)
	private String location;

	@Column(name = "registration_required", nullable = false)
	private boolean registrationRequired;

	// Nullable — no capacity limit when absent.
	@Column(name = "capacity")
	private Integer capacity;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Event create(String title, String description, LocalDateTime eventDate, String location,
			boolean registrationRequired, Integer capacity) {
		return Event.builder()
				.title(title)
				.description(description)
				.eventDate(eventDate)
				.location(location)
				.registrationRequired(registrationRequired)
				.capacity(capacity)
				.active(true)
				.build();
	}

	public void updateDetails(String title, String description, LocalDateTime eventDate, String location) {
		this.title = title;
		this.description = description;
		this.eventDate = eventDate;
		this.location = location;
	}

	public void cancel() {
		this.active = false;
	}
}
