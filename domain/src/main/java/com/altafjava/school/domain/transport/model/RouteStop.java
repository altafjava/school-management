package com.altafjava.school.domain.transport.model;

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
@Table(name = "route_stops")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class RouteStop extends SoftDeletableEntity {

	@Column(name = "route_id", nullable = false)
	private Long routeId;

	@Column(name = "stop_name", nullable = false, length = 150)
	private String stopName;

	@Column(name = "sequence_order", nullable = false)
	private int sequenceOrder;

	@Column(name = "pickup_time")
	private LocalTime pickupTime;

	@Column(name = "drop_time")
	private LocalTime dropTime;

	public static RouteStop create(Long routeId, String stopName, int sequenceOrder, LocalTime pickupTime,
			LocalTime dropTime) {
		return RouteStop.builder()
				.routeId(routeId)
				.stopName(stopName)
				.sequenceOrder(sequenceOrder)
				.pickupTime(pickupTime)
				.dropTime(dropTime)
				.build();
	}
}
