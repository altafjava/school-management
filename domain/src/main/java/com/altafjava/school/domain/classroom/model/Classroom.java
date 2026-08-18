package com.altafjava.school.domain.classroom.model;

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
@Table(name = "classrooms")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Classroom extends SoftDeletableEntity {

	@Column(name = "class_code", nullable = false, length = 50)
	private String classCode;

	@Column(name = "grade", nullable = false, length = 20)
	private String grade;

	@Column(name = "section", nullable = false, length = 10)
	private String section;

	// Deprecated — superseded by academicYearId (a real FK). Kept only for the migration/rollback
	// window (see ROADMAP.md Phase 1.1 cleanup); never read for new logic, only written to keep
	// legacy readers of this column working during the transition.
	@Column(name = "academic_year", nullable = false, length = 20)
	private String academicYear;

	// FK to academic_years.id — stored as Long to avoid cross-entity coupling in domain layer
	@Column(name = "academic_year_id", nullable = false)
	private Long academicYearId;

	// FK to teachers.id — stored as Long to avoid cross-entity coupling in domain layer
	@Column(name = "class_teacher_id")
	private Long classTeacherId;

	public static Classroom create(String classCode, String grade, String section,
			Long academicYearId, String academicYearName, Long classTeacherId) {
		return Classroom.builder()
				.classCode(classCode)
				.grade(grade)
				.section(section)
				.academicYearId(academicYearId)
				.academicYear(academicYearName)
				.classTeacherId(classTeacherId)
				.build();
	}

	public void reassignTeacher(Long classTeacherId) {
		this.classTeacherId = classTeacherId;
	}

	public void reassignAcademicYear(Long academicYearId, String academicYearName) {
		this.academicYearId = academicYearId;
		this.academicYear = academicYearName;
	}
}
