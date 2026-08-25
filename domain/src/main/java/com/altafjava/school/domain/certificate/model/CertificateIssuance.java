package com.altafjava.school.domain.certificate.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A single generated-and-stored certificate PDF for a student, resolved from a
 * {@link CertificateTemplate} at issuance time. {@code verificationCode} is short, URL-safe, and
 * collision-checked at creation (see {@code CertificateService#generateVerificationCode}) — it is
 * the only thing a certificate-verification caller needs to confirm authenticity, so it is looked
 * up independently of the surrogate/public id.
 */
@Entity
@Table(name = "certificate_issuances")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CertificateIssuance extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to certificate_templates.id
	@Column(name = "certificate_template_id", nullable = false)
	private Long certificateTemplateId;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "verification_code", nullable = false, length = 32)
	private String verificationCode;

	// Object storage key (platform StorageService) where the generated PDF is stored.
	@Column(name = "storage_key", nullable = false, length = 500)
	private String storageKey;

	// FK to platform users.id — the staff member who issued this certificate.
	@Column(name = "issued_by_user_id", nullable = false)
	private Long issuedByUserId;

	public static CertificateIssuance create(Long studentId, Long certificateTemplateId, String verificationCode,
			String storageKey, Long issuedByUserId) {
		return CertificateIssuance.builder()
				.studentId(studentId)
				.certificateTemplateId(certificateTemplateId)
				.issuedAt(Instant.now())
				.verificationCode(verificationCode)
				.storageKey(storageKey)
				.issuedByUserId(issuedByUserId)
				.build();
	}
}
