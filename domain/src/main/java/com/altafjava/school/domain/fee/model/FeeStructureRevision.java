package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Append-only audit trail for {@link FeeStructure#reviseAmount} — one row per revision, capturing
 * the before/after amount so a fee-schedule dispute can be answered from history data rather than
 * just {@code updatedBy}/{@code updatedAt} showing a change happened with no record of what it was.
 */
@Entity
@Table(name = "fee_structure_revisions")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class FeeStructureRevision extends SoftDeletableEntity {

	// FK to fee_structures.id
	@Column(name = "fee_structure_id", nullable = false)
	private Long feeStructureId;

	@Column(name = "old_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal oldAmount;

	@Column(name = "new_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal newAmount;

	public static FeeStructureRevision record(Long feeStructureId, BigDecimal oldAmount, BigDecimal newAmount) {
		return FeeStructureRevision.builder()
				.feeStructureId(feeStructureId)
				.oldAmount(oldAmount)
				.newAmount(newAmount)
				.build();
	}
}
