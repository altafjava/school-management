package com.altafjava.school.domain.timetable.model;

import java.time.LocalTime;
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
@Table(name = "periods")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Period extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	public static Period create(String name, LocalTime startTime, LocalTime endTime, int displayOrder) {
		return Period.builder()
				.name(name)
				.startTime(startTime)
				.endTime(endTime)
				.displayOrder(displayOrder)
				.build();
	}
}
