package com.altafjava.school.domain.student.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "students")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Student extends SoftDeletableEntity {

	@Pii
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Pii
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Pii
	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "student_code", nullable = false, length = 50)
	private String studentCode;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	@Enumerated(EnumType.STRING)
	@Column(name = "enrollment_status", nullable = false, length = 30)
	private EnrollmentStatus enrollmentStatus;

	// FK to platform users.id — nullable, set only once this student has their own login account.
	@Column(name = "user_id")
	private Long userId;

	public static Student create(String studentCode, String firstName, String lastName,
			String email, LocalDate dateOfBirth) {
		return Student.builder()
				.studentCode(studentCode)
				.firstName(firstName)
				.lastName(lastName)
				.email(email)
				.dateOfBirth(dateOfBirth)
				.enrollmentStatus(EnrollmentStatus.ACTIVE)
				.build();
	}

	public void withdraw() {
		if (this.enrollmentStatus == EnrollmentStatus.GRADUATED) {
			throw new BusinessException("Cannot withdraw a graduated student");
		}
		this.enrollmentStatus = EnrollmentStatus.WITHDRAWN;
	}

	public void graduate() {
		if (this.enrollmentStatus == EnrollmentStatus.WITHDRAWN) {
			throw new BusinessException("Cannot graduate a withdrawn student");
		}
		this.enrollmentStatus = EnrollmentStatus.GRADUATED;
	}

	public void updateContactDetails(String firstName, String lastName, String email, LocalDate dateOfBirth) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.dateOfBirth = dateOfBirth;
	}
}
