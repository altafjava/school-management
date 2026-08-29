package com.altafjava.school.domain.exam.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class ExamTest {

	private Exam newExam() {
		return Exam.create("Midterm", 5L, 10L, LocalDateTime.now().plusDays(7), BigDecimal.valueOf(100), null, 1L);
	}

	@Test
	void create_defaultsStatusToScheduled() {
		Exam exam = newExam();

		assertEquals(ExamStatus.SCHEDULED, exam.getStatus());
	}

	@Test
	void complete_scheduledExam_transitionsToCompleted() {
		Exam exam = newExam();

		exam.complete();

		assertEquals(ExamStatus.COMPLETED, exam.getStatus());
	}

	@Test
	void complete_cancelledExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.cancel();

		assertThrows(BusinessException.class, exam::complete);
	}

	@Test
	void complete_alreadyCompletedExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.complete();

		assertThrows(BusinessException.class, exam::complete);
	}

	@Test
	void cancel_scheduledExam_transitionsToCancelled() {
		Exam exam = newExam();

		exam.cancel();

		assertEquals(ExamStatus.CANCELLED, exam.getStatus());
	}

	@Test
	void cancel_completedExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.complete();

		assertThrows(BusinessException.class, exam::cancel);
	}

	@Test
	void cancel_alreadyCancelledExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.cancel();

		assertThrows(BusinessException.class, exam::cancel);
	}
}
