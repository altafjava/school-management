package com.altafjava.school.domain.fee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fee_assignments")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FeeAssignment extends SoftDeletableEntity {

	// FK to fee_structures.id
	@Column(name = "fee_structure_id", nullable = false)
	private Long feeStructureId;

	@Enumerated(EnumType.STRING)
	@Column(name = "scope", nullable = false, length = 20)
	private FeeAssignmentScope scope;

	// FK to students.id — set iff scope == STUDENT
	@Column(name = "student_id")
	private Long studentId;

	// FK to classrooms.id — set iff scope == CLASSROOM
	@Column(name = "classroom_id")
	private Long classroomId;

	public static FeeAssignment forStudent(Long feeStructureId, Long studentId) {
		return FeeAssignment.builder()
				.feeStructureId(feeStructureId)
				.scope(FeeAssignmentScope.STUDENT)
				.studentId(studentId)
				.build();
	}

	public static FeeAssignment forClassroom(Long feeStructureId, Long classroomId) {
		return FeeAssignment.builder()
				.feeStructureId(feeStructureId)
				.scope(FeeAssignmentScope.CLASSROOM)
				.classroomId(classroomId)
				.build();
	}
}
