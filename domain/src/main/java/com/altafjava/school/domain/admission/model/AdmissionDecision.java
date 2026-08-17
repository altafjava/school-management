package com.altafjava.school.domain.admission.model;

import java.time.Instant;
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
@Table(name = "admission_decisions")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AdmissionDecision extends SoftDeletableEntity {

	// FK to admissions.id
	@Column(name = "admission_id", nullable = false)
	private Long admissionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "outcome", nullable = false, length = 20)
	private DecisionOutcome outcome;

	@Column(name = "decided_by", length = 100)
	private String decidedBy;

	@Column(name = "decided_at", nullable = false)
	private Instant decidedAt;

	@Column(name = "notes", length = 1000)
	private String notes;

	public static AdmissionDecision record(Long admissionId, DecisionOutcome outcome, String decidedBy,
			String notes) {
		return AdmissionDecision.builder()
				.admissionId(admissionId)
				.outcome(outcome)
				.decidedBy(decidedBy)
				.decidedAt(Instant.now())
				.notes(notes)
				.build();
	}
}
