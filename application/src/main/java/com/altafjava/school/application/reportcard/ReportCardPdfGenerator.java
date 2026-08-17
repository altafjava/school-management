package com.altafjava.school.application.reportcard;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.term.model.Term;

// Renders a student's term grades into a PDF byte array. Pure formatting — no persistence,
// no I/O beyond the in-memory buffer, so it stays independently unit-testable.
@Component
public class ReportCardPdfGenerator {

	private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
	private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
	private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);

	public byte[] generate(Student student, Term term, List<ReportCardLine> lines) {
		Document document = new Document();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(document, out);
			document.open();

			document.add(new Paragraph("Report Card", TITLE_FONT));
			document.add(new Paragraph(" "));
			document.add(new Paragraph(
					"Student: " + student.getFirstName() + " " + student.getLastName()
							+ " (" + student.getStudentCode() + ")",
					BODY_FONT));
			document.add(new Paragraph("Term: " + term.getName(), BODY_FONT));
			document.add(new Paragraph(" "));

			PdfPTable table = new PdfPTable(4);
			table.setWidthPercentage(100);
			addHeaderCell(table, "Subject");
			addHeaderCell(table, "Exam");
			addHeaderCell(table, "Marks");
			addHeaderCell(table, "Grade");

			for (ReportCardLine line : lines) {
				addBodyCell(table, line.subjectName());
				addBodyCell(table, line.examTitle());
				addBodyCell(table, line.marks() + " / " + line.maxMarks());
				addBodyCell(table, line.gradeLetter());
			}
			document.add(table);
		} catch (DocumentException e) {
			throw new BusinessException("Failed to generate report card PDF: " + e.getMessage());
		} finally {
			document.close();
		}
		return out.toByteArray();
	}

	private void addHeaderCell(PdfPTable table, String text) {
		PdfPCell cell = new PdfPCell(new Paragraph(new Chunk(text, HEADER_FONT)));
		table.addCell(cell);
	}

	private void addBodyCell(PdfPTable table, String text) {
		PdfPCell cell = new PdfPCell(new Paragraph(new Chunk(text, BODY_FONT)));
		table.addCell(cell);
	}
}
