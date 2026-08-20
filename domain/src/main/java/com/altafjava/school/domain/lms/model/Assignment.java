package com.altafjava.school.domain.lms.model;

import java.math.BigDecimal;
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

@Entity
@Table(name = "assignments")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Assignment extends SoftDeletableEntity {

	// FK to classrooms.id
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	// FK to subjects.id
	@Column(name = "subject_id", nullable = false)
	private Long subjectId;

	// FK to teachers.id — the assigning teacher
	@Column(name = "teacher_id", nullable = false)
	private Long teacherId;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	// Object storage key (platform StorageService) for an optional single attachment.
	@Column(name = "storage_key", length = 500)
	private String storageKey;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "max_marks", precision = 10, scale = 2)
	private BigDecimal maxMarks;

	public static Assignment create(Long classroomId, Long subjectId, Long teacherId, String title,
			String description, String storageKey, LocalDate dueDate, BigDecimal maxMarks) {
		return Assignment.builder()
				.classroomId(classroomId)
				.subjectId(subjectId)
				.teacherId(teacherId)
				.title(title)
				.description(description)
				.storageKey(storageKey)
				.dueDate(dueDate)
				.maxMarks(maxMarks)
				.build();
	}

	public void reschedule(LocalDate newDueDate) {
		this.dueDate = newDueDate;
	}
}
