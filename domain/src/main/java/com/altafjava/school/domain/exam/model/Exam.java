package com.altafjava.school.domain.exam.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
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

	// FK to terms.id — nullable: existing exams predate this field and have no reliable source
	// to backfill from (ReportCardService derives term membership from scheduledAt, not an FK).
	@Column(name = "term_id")
	private Long termId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ExamStatus status;

	// FK to exam_type_definitions.id — nullable only at the DB level; create()/ExamService
	// require and validate a real value.
	@Column(name = "exam_type_id")
	private Long examTypeId;

	public static Exam create(String title, Long subjectId, Long classroomId,
			LocalDateTime scheduledAt, BigDecimal maxMarks, Long termId, Long examTypeId) {
		return Exam.builder()
				.title(title)
				.subjectId(subjectId)
				.classroomId(classroomId)
				.scheduledAt(scheduledAt)
				.maxMarks(maxMarks)
				.termId(termId)
				.examTypeId(examTypeId)
				.status(ExamStatus.SCHEDULED)
				.build();
	}

	public void reschedule(LocalDateTime scheduledAt) {
		this.scheduledAt = scheduledAt;
	}

	public void assignTerm(Long termId) {
		this.termId = termId;
	}

	public void complete() {
		if (this.status == ExamStatus.CANCELLED) {
			throw new BusinessException("Cannot complete a cancelled exam");
		}
		if (this.status == ExamStatus.COMPLETED) {
			throw new BusinessException("Exam is already completed");
		}
		this.status = ExamStatus.COMPLETED;
	}

	public void cancel() {
		if (this.status == ExamStatus.COMPLETED) {
			throw new BusinessException("Cannot cancel a completed exam");
		}
		if (this.status == ExamStatus.CANCELLED) {
			throw new BusinessException("Exam is already cancelled");
		}
		this.status = ExamStatus.CANCELLED;
	}
}
