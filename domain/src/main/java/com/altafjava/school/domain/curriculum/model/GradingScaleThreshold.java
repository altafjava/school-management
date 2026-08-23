package com.altafjava.school.domain.curriculum.model;

import java.math.BigDecimal;
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
 * One step of a {@link GradingScale}: {@code letter} is awarded when a percentage is at least
 * {@code minPercentage}, and carries a {@code points} value for GPA calculation (e.g. A=4.0). A
 * separate table with a plain {@code gradingScaleId} FK — not a JPA {@code @OneToMany} on
 * GradingScale — matching this codebase's house style of no ORM-managed associations (see
 * {@code Assignment}/{@code Lesson} in domain/lms for the precedent).
 */
@Entity
@Table(name = "grading_scale_thresholds")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class GradingScaleThreshold extends SoftDeletableEntity {

	@Column(name = "grading_scale_id", nullable = false)
	private Long gradingScaleId;

	@Column(name = "letter", nullable = false, length = 5)
	private String letter;

	@Column(name = "min_percentage", nullable = false, precision = 6, scale = 2)
	private BigDecimal minPercentage;

	@Column(name = "points", nullable = false, precision = 4, scale = 2)
	private BigDecimal points;

	public static GradingScaleThreshold create(Long gradingScaleId, String letter, BigDecimal minPercentage,
			BigDecimal points) {
		return GradingScaleThreshold.builder()
				.gradingScaleId(gradingScaleId)
				.letter(letter)
				.minPercentage(minPercentage)
				.points(points)
				.build();
	}
}
