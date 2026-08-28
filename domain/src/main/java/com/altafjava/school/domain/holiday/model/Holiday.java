package com.altafjava.school.domain.holiday.model;

import java.time.LocalDate;
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
 * A tenant's school-calendar holiday — excluded from attendance-percentage denominators and
 * leave-day counts (see {@code HolidayService#datesInRange}). A {@code recurring} holiday (e.g. a
 * fixed national day) repeats every year on the same month/day regardless of the year stored in
 * {@link #date}; a non-recurring one is a single specific date (e.g. a one-off local event).
 */
@Entity
@Table(name = "holidays")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Holiday extends SoftDeletableEntity {

	@Column(name = "date", nullable = false)
	private LocalDate date;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "recurring", nullable = false)
	private boolean recurring;

	public static Holiday create(LocalDate date, String name, boolean recurring) {
		return Holiday.builder()
				.date(date)
				.name(name)
				.recurring(recurring)
				.build();
	}

	public void updateDetails(LocalDate date, String name, boolean recurring) {
		this.date = date;
		this.name = name;
		this.recurring = recurring;
	}

	public boolean fallsOn(LocalDate candidate) {
		if (recurring) {
			return date.getMonthValue() == candidate.getMonthValue()
					&& date.getDayOfMonth() == candidate.getDayOfMonth();
		}
		return date.equals(candidate);
	}
}
