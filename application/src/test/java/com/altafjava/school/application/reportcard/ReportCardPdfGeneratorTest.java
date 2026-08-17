package com.altafjava.school.application.reportcard;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.term.model.Term;

class ReportCardPdfGeneratorTest {

	private final ReportCardPdfGenerator generator = new ReportCardPdfGenerator();

	@Test
	void generate_withLines_producesNonEmptyValidPdf() {
		Student student = Student.create("STU-001", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		Term term = Term.create("Term 1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), 1L);
		List<ReportCardLine> lines = List.of(
				new ReportCardLine("Mathematics", "Midterm", BigDecimal.valueOf(85), BigDecimal.valueOf(100), "A"));

		byte[] pdf = generator.generate(student, term, lines);

		assertTrue(pdf.length > 0, "Generated PDF must not be empty");
		String header = new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
		assertTrue(header.equals("%PDF"), "Output must be a real PDF (start with %PDF magic bytes)");
	}

	@Test
	void generate_withNoLines_stillProducesValidPdf() {
		Student student = Student.create("STU-002", "Bob", "Jones", "bob@school.test", LocalDate.of(2011, 2, 2));
		Term term = Term.create("Term 1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), 1L);

		byte[] pdf = generator.generate(student, term, List.of());

		assertTrue(pdf.length > 0);
		String header = new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
		assertTrue(header.equals("%PDF"));
	}
}
