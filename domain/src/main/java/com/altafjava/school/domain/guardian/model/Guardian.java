package com.altafjava.school.domain.guardian.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "guardians")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Guardian extends SoftDeletableEntity {

	@Pii
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Pii
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Pii
	@Column(name = "email", length = 255)
	private String email;

	@Pii
	@Column(name = "phone", length = 30)
	private String phone;

	// FK to platform users.id — nullable, set only once this guardian has a login account.
	@Column(name = "user_id")
	private Long userId;

	public static Guardian create(String firstName, String lastName, String email, String phone, Long userId) {
		return Guardian.builder()
				.firstName(firstName)
				.lastName(lastName)
				.email(email)
				.phone(phone)
				.userId(userId)
				.build();
	}

	public void linkUserAccount(Long userId) {
		if (this.userId != null) {
			throw new BusinessException("Guardian is already linked to a user account");
		}
		this.userId = userId;
	}
}
