package com.altafjava.school.domain.reportcard.model;

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

@Entity
@Table(name = "report_cards")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ReportCard extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to terms.id
	@Column(name = "term_id", nullable = false)
	private Long termId;

	// Object storage key (platform StorageService) where the generated PDF is stored.
	@Column(name = "storage_key", nullable = false, length = 500)
	private String storageKey;

	@Column(name = "generated_at", nullable = false)
	private Instant generatedAt;

	public static ReportCard create(Long studentId, Long termId, String storageKey) {
		return ReportCard.builder()
				.studentId(studentId)
				.termId(termId)
				.storageKey(storageKey)
				.generatedAt(Instant.now())
				.build();
	}
}
