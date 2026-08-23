package com.altafjava.school.domain.leave.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class LeaveRequestTest {

	private LeaveRequest submitted(LocalDate startDate, LocalDate endDate) {
		return LeaveRequest.submit(1L, 2L, 3L, startDate, endDate, "Family event");
	}

	@Test
	void submit_computesInclusiveDayCount() {
		LeaveRequest request = submitted(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3));

		assertEquals(0, BigDecimal.valueOf(3).compareTo(request.getDaysRequested()));
		assertEquals(LeaveRequestStatus.PENDING, request.getStatus());
	}

	@Test
	void submit_withEndDateBeforeStartDate_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> submitted(LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 1)));
	}

	@Test
	void approve_fromPending_setsApprovedStatus() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

		request.approve(99L);

		assertEquals(LeaveRequestStatus.APPROVED, request.getStatus());
		assertEquals(99L, request.getApprovedByUserId());
	}

	@Test
	void approve_whenNotPending_throwsBusinessException() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
		request.approve(99L);

		assertThrows(BusinessException.class, () -> request.approve(99L));
	}

	@Test
	void reject_fromPending_setsRejectedStatusWithReason() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

		request.reject(99L, "Insufficient staffing on those dates");

		assertEquals(LeaveRequestStatus.REJECTED, request.getStatus());
		assertEquals("Insufficient staffing on those dates", request.getRejectionReason());
	}

	@Test
	void cancel_whenPendingAndFuture_setsCancelledStatus() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(5), LocalDate.now().plusDays(6));

		request.cancel();

		assertEquals(LeaveRequestStatus.CANCELLED, request.getStatus());
	}

	@Test
	void cancel_whenAlreadyRejected_throwsBusinessException() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
		request.reject(99L, "No");

		assertThrows(BusinessException.class, request::cancel);
	}

	@Test
	void cancel_whenAlreadyStarted_throwsBusinessException() {
		LeaveRequest request = submitted(LocalDate.now().minusDays(3), LocalDate.now().plusDays(1));

		assertThrows(BusinessException.class, request::cancel);
	}
}
