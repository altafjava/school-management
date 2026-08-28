package com.altafjava.school.domain.reportcard.model;

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
 * One per tenant — which optional sections {@code ReportCardPdfGenerator} renders. Every flag
 * defaults false so a tenant that never configures this keeps the exact prior fixed-section output
 * (header/student-info/grades/summary/signature/footer only).
 */
@Entity
@Table(name = "report_card_templates")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ReportCardTemplate extends SoftDeletableEntity {

	@Column(name = "show_attendance_summary", nullable = false)
	private boolean showAttendanceSummary;

	@Column(name = "show_remarks", nullable = false)
	private boolean showRemarks;

	@Column(name = "show_competency_grid", nullable = false)
	private boolean showCompetencyGrid;

	@Column(name = "show_rank", nullable = false)
	private boolean showRank;

	public static ReportCardTemplate createDefault() {
		return ReportCardTemplate.builder()
				.showAttendanceSummary(false)
				.showRemarks(false)
				.showCompetencyGrid(false)
				.showRank(false)
				.build();
	}

	public void configure(boolean showAttendanceSummary, boolean showRemarks, boolean showCompetencyGrid,
			boolean showRank) {
		this.showAttendanceSummary = showAttendanceSummary;
		this.showRemarks = showRemarks;
		this.showCompetencyGrid = showCompetencyGrid;
		this.showRank = showRank;
	}
}
