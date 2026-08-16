package com.altafjava.school.domain.exam.model;

import java.math.BigDecimal;
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
@Table(name = "exams")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Exam extends SoftDeletableEntity {

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	// FK to subjects.id
	@Column(name = "subject_id", nullable = false)
	private Long subjectId;

	// FK to classrooms.id
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt;

	@Column(name = "max_marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal maxMarks;

	public static Exam create(String title, Long subjectId, Long classroomId,
			LocalDateTime scheduledAt, BigDecimal maxMarks) {
		return Exam.builder()
				.title(title)
				.subjectId(subjectId)
				.classroomId(classroomId)
				.scheduledAt(scheduledAt)
				.maxMarks(maxMarks)
				.build();
	}
}
