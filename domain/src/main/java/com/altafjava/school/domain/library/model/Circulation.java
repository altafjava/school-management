package com.altafjava.school.domain.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "circulations")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Circulation extends SoftDeletableEntity {

	@Column(name = "book_copy_id", nullable = false)
	private Long bookCopyId;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "checked_out_at", nullable = false)
	private LocalDate checkedOutAt;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "returned_at")
	private LocalDate returnedAt;

	@Column(name = "fine_amount", precision = 10, scale = 2)
	private BigDecimal fineAmount;

	public static Circulation checkout(Long bookCopyId, Long studentId, LocalDate checkedOutAt, LocalDate dueDate) {
		return Circulation.builder()
				.bookCopyId(bookCopyId)
				.studentId(studentId)
				.checkedOutAt(checkedOutAt)
				.dueDate(dueDate)
				.build();
	}

	public void returnBook(LocalDate returnedAt, BigDecimal fineAmount) {
		if (this.returnedAt != null) {
			throw new BusinessException("Circulation already returned on " + this.returnedAt);
		}
		this.returnedAt = returnedAt;
		this.fineAmount = fineAmount;
	}

	public boolean isOverdue(LocalDate asOf) {
		return this.returnedAt == null && asOf.isAfter(this.dueDate);
	}
}
