package com.altafjava.school.domain.grade.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Append-only audit trail for {@link Grade} corrections — one row per correction, capturing the
 * before/after marks and letter grade so "what was this grade before the teacher's correction on
 * date X" is reconstructable. {@code Grade} itself only tracks the current value plus
 * {@code updatedBy}/{@code updatedAt} (via {@code BaseEntity}), which shows a change happened but
 * not what it was.
 */
@Entity
@Table(name = "grade_corrections")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class GradeCorrection extends SoftDeletableEntity {

	// FK to grades.id
	@Column(name = "grade_id", nullable = false)
	private Long gradeId;

	@Column(name = "old_marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal oldMarks;

	@Column(name = "old_grade_letter", nullable = false, length = 5)
	private String oldGradeLetter;

	@Column(name = "new_marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal newMarks;

	@Column(name = "new_grade_letter", nullable = false, length = 5)
	private String newGradeLetter;

	public static GradeCorrection record(Long gradeId, BigDecimal oldMarks, String oldGradeLetter,
			BigDecimal newMarks, String newGradeLetter) {
		return GradeCorrection.builder()
				.gradeId(gradeId)
				.oldMarks(oldMarks)
				.oldGradeLetter(oldGradeLetter)
				.newMarks(newMarks)
				.newGradeLetter(newGradeLetter)
				.build();
	}
}
