package com.altafjava.school.domain.teacher.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "teachers")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Teacher extends SoftDeletableEntity {

	@Column(name = "employee_code", nullable = false, length = 50)
	private String employeeCode;

	@Pii
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Pii
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Pii
	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "join_date")
	private LocalDate joinDate;

	// FK to platform users.id — nullable, set only once this teacher has a login account.
	@Column(name = "user_id")
	private Long userId;

	// FK to departments.id — nullable, HR details are assigned after hiring, not at hire time.
	@Column(name = "department_id")
	private Long departmentId;

	@Column(name = "qualification", length = 255)
	private String qualification;

	@Enumerated(EnumType.STRING)
	@Column(name = "employment_type", length = 30)
	private EmploymentType employmentType;

	public static Teacher create(String employeeCode, String firstName, String lastName,
			String email, LocalDate joinDate) {
		return Teacher.builder()
				.employeeCode(employeeCode)
				.firstName(firstName)
				.lastName(lastName)
				.email(email)
				.joinDate(joinDate)
				.build();
	}

	public void updateContactDetails(String firstName, String lastName, String email) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	public void assignHrDetails(Long departmentId, String qualification, EmploymentType employmentType) {
		this.departmentId = departmentId;
		this.qualification = qualification;
		this.employmentType = employmentType;
	}
}
