package com.altafjava.school.domain.alumni.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * {@code studentId} links back to the historical {@code Student} row rather than duplicating
 * name/DOB/etc. — Phase 5 decoupled {@code Student.graduate()} from soft delete, so a graduated
 * student's row stays queryable (and un-deleted) indefinitely.
 *
 * <p>
 * Created explicitly by staff action ({@code AlumniProfileService.create}), not auto-triggered off
 * {@code Student.graduate()}: {@code Student} publishes no domain event on graduation today
 * ({@code StudentService.graduate()} just flips {@code enrollmentStatus}), and adding one purely for
 * this module is out of proportion to the gain — the plan's suggested derivation source for
 * {@code graduationYear} (the academic year active at graduation time) also isn't reliably available
 * at that call site, since {@code Student.graduate()} takes no academic-year parameter today. An
 * explicit staff-supplied {@code graduationYear} at profile-creation time is simpler and just as
 * correct, and {@code create()} still enforces that the student's {@code enrollmentStatus} is
 * {@code GRADUATED} before a profile can be created.
 *
 * <p>
 * Alumni event participation reuses the existing {@code Event}/{@code EventRegistration} module
 * as-is: {@code EventRegistration.studentId} is the same underlying (non-deleted) {@code Student} id
 * this profile references, and {@code EventRegistrationService.register} performs no
 * enrollment-status check — so an alumnus can already register for events via their original student
 * public id with zero changes to that module.
 *
 * <p>
 * {@code active} tracks alumni-contact opt-in/opt-out — a business concern distinct from the
 * soft-delete flag, mirroring {@code Book.active}/{@code Subject.active}.
 */
@Entity
@Table(name = "alumni_profiles")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AlumniProfile extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false, unique = true)
	private Long studentId;

	@Column(name = "graduation_year", nullable = false)
	private int graduationYear;

	@Column(name = "current_occupation", length = 255)
	private String currentOccupation;

	@Pii
	@Column(name = "contact_email", length = 255)
	private String contactEmail;

	@Pii
	@Column(name = "contact_phone", length = 50)
	private String contactPhone;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static AlumniProfile create(Long studentId, int graduationYear, String currentOccupation,
			String contactEmail, String contactPhone) {
		return AlumniProfile.builder()
				.studentId(studentId)
				.graduationYear(graduationYear)
				.currentOccupation(currentOccupation)
				.contactEmail(contactEmail)
				.contactPhone(contactPhone)
				.active(true)
				.build();
	}

	public void updateContactInfo(String currentOccupation, String contactEmail, String contactPhone) {
		this.currentOccupation = currentOccupation;
		this.contactEmail = contactEmail;
		this.contactPhone = contactPhone;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
