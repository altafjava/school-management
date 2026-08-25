package com.altafjava.school.domain.certificate.model;

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
 * Tenant-defined certificate type (e.g. "Bonafide Certificate", "Transfer Certificate") — schools
 * vary widely in wording and required fields, so this is a runtime catalog rather than a hardcoded
 * enum, mirroring {@code LeaveType}/{@code Department}. {@code bodyTemplate} uses the same
 * {@code {{placeholder}}} token convention as platform's {@code NotificationTemplate.bodyTemplate}
 * (see {@code CertificatePlaceholderResolver}).
 */
@Entity
@Table(name = "certificate_templates")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CertificateTemplate extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
	private String bodyTemplate;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static CertificateTemplate create(String name, String bodyTemplate) {
		return CertificateTemplate.builder()
				.name(name)
				.bodyTemplate(bodyTemplate)
				.active(true)
				.build();
	}

	public void updateDetails(String name, String bodyTemplate) {
		this.name = name;
		this.bodyTemplate = bodyTemplate;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
