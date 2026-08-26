package com.altafjava.school.application.reportcard;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.term.model.Term;

/**
 * Renders a student's term grades into a branded PDF byte array. Pure formatting — no persistence,
 * no I/O beyond the in-memory buffer (and the caller-supplied logo bytes, already resolved), so it
 * stays independently unit-testable.
 */
@Component
public class ReportCardPdfGenerator {

	private static final Color BRAND_NAVY = new Color(30, 58, 95);
	private static final Color HEADER_ROW_TEXT = Color.WHITE;
	private static final Color ROW_STRIPE = new Color(240, 243, 247);
	private static final Color BORDER_GRAY = new Color(200, 200, 200);
	private static final Color MUTED_TEXT = new Color(110, 110, 110);

	private static final Font TENANT_NAME_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, BRAND_NAVY);
	private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, BRAND_NAVY);
	private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
	private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
	private static final Font HEADER_ROW_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, HEADER_ROW_TEXT);
	private static final Font SUMMARY_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, BRAND_NAVY);
	private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.ITALIC, MUTED_TEXT);
	private static final Font SIGNATURE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);

	private static final float LOGO_MAX_HEIGHT = 48f;
	private static final DateTimeFormatter GENERATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
			.withZone(ZoneOffset.UTC);

	public byte[] generate(Student student, Term term, List<ReportCardLine> lines, String tenantName,
			byte[] logoBytes) {
		Document document = new Document(com.lowagie.text.PageSize.A4, 40, 40, 40, 40);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(document, out);
			document.open();

			addHeader(document, tenantName, logoBytes);
			addStudentInfo(document, student, term);
			addGradesTable(document, lines);
			addSummary(document, lines);
			addSignatureBlock(document);
			addFooter(document);
		} catch (DocumentException e) {
			throw new BusinessException("Failed to generate report card PDF: " + e.getMessage());
		} finally {
			document.close();
		}
		return out.toByteArray();
	}

	private void addHeader(Document document, String tenantName, byte[] logoBytes) throws DocumentException {
		PdfPTable header = new PdfPTable(logoBytes != null ? 2 : 1);
		header.setWidthPercentage(100);
		if (logoBytes != null) {
			try {
				header.setWidths(new float[] { 1f, 4f });
				Image logo = Image.getInstance(logoBytes);
				logo.scaleToFit(LOGO_MAX_HEIGHT * 2, LOGO_MAX_HEIGHT);
				PdfPCell logoCell = new PdfPCell(logo, false);
				logoCell.setBorder(Rectangle.NO_BORDER);
				logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				header.addCell(logoCell);
			} catch (BadElementException | IOException e) {
				// A corrupt/unreadable logo must never block report-card generation — fall back to
				// the text-only header exactly as if no logo were configured at all.
				header = new PdfPTable(1);
				header.setWidthPercentage(100);
			}
		}
		PdfPCell titleCell = new PdfPCell();
		titleCell.setBorder(Rectangle.NO_BORDER);
		titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		titleCell.addElement(new Paragraph(tenantName, TENANT_NAME_FONT));
		titleCell.addElement(new Paragraph("Report Card", TITLE_FONT));
		header.addCell(titleCell);
		document.add(header);

		document.add(rule());
		document.add(new Paragraph(" "));
	}

	private void addStudentInfo(Document document, Student student, Term term) throws DocumentException {
		PdfPTable info = new PdfPTable(2);
		info.setWidthPercentage(100);
		info.setWidths(new float[] { 1f, 1f });
		addInfoCell(info, "Student", student.getFirstName() + " " + student.getLastName());
		addInfoCell(info, "Student Code", student.getStudentCode());
		addInfoCell(info, "Term", term.getName());
		addInfoCell(info, "Generated", GENERATED_AT_FORMAT.format(java.time.Instant.now()));
		document.add(info);
		document.add(new Paragraph(" "));
	}

	private void addInfoCell(PdfPTable table, String label, String value) {
		PdfPCell cell = new PdfPCell();
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setPadding(4f);
		Paragraph p = new Paragraph();
		p.add(new Chunk(label + ": ", LABEL_FONT));
		p.add(new Chunk(value, BODY_FONT));
		cell.addElement(p);
		table.addCell(cell);
	}

	private void addGradesTable(Document document, List<ReportCardLine> lines) throws DocumentException {
		PdfPTable table = new PdfPTable(4);
		table.setWidthPercentage(100);
		table.setWidths(new float[] { 2.5f, 2.5f, 1.5f, 1f });

		addHeaderCell(table, "Subject");
		addHeaderCell(table, "Exam");
		addHeaderCell(table, "Marks");
		addHeaderCell(table, "Grade");

		boolean stripe = false;
		for (ReportCardLine line : lines) {
			Color background = stripe ? ROW_STRIPE : Color.WHITE;
			addBodyCell(table, line.subjectName(), background);
			addBodyCell(table, line.examTitle(), background);
			addBodyCell(table, line.marks() + " / " + line.maxMarks(), background);
			addBodyCell(table, line.gradeLetter(), background);
			stripe = !stripe;
		}
		document.add(table);
		document.add(new Paragraph(" "));
	}

	private void addHeaderCell(PdfPTable table, String text) {
		PdfPCell cell = new PdfPCell(new Paragraph(new Chunk(text, HEADER_ROW_FONT)));
		cell.setBackgroundColor(BRAND_NAVY);
		cell.setPadding(6f);
		cell.setBorderColor(BRAND_NAVY);
		table.addCell(cell);
	}

	private void addBodyCell(PdfPTable table, String text, Color background) {
		PdfPCell cell = new PdfPCell(new Paragraph(new Chunk(text, BODY_FONT)));
		cell.setBackgroundColor(background);
		cell.setBorderColor(BORDER_GRAY);
		cell.setPadding(6f);
		table.addCell(cell);
	}

	/**
	 * Total/max marks and overall percentage across every line — an empty result set (no grades
	 * recorded yet for this term) omits the box entirely rather than showing a misleading 0%.
	 */
	private void addSummary(Document document, List<ReportCardLine> lines) throws DocumentException {
		if (lines.isEmpty()) {
			return;
		}
		BigDecimal totalMarks = lines.stream().map(ReportCardLine::marks).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalMax = lines.stream().map(ReportCardLine::maxMarks).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (totalMax.compareTo(BigDecimal.ZERO) == 0) {
			return;
		}
		BigDecimal percentage = totalMarks.multiply(BigDecimal.valueOf(100))
				.divide(totalMax, 2, RoundingMode.HALF_UP);

		PdfPTable summary = new PdfPTable(1);
		summary.setWidthPercentage(45);
		summary.setHorizontalAlignment(Element.ALIGN_LEFT);
		PdfPCell cell = new PdfPCell();
		cell.setPadding(8f);
		cell.setBorderColor(BRAND_NAVY);
		cell.addElement(new Paragraph("Total: " + totalMarks + " / " + totalMax, SUMMARY_FONT));
		cell.addElement(new Paragraph("Percentage: " + percentage + "%", SUMMARY_FONT));
		summary.addCell(cell);
		document.add(summary);
		document.add(new Paragraph(" "));
	}

	private void addSignatureBlock(Document document) throws DocumentException {
		document.add(new Paragraph(" "));
		PdfPTable signatures = new PdfPTable(2);
		signatures.setWidthPercentage(100);
		signatures.setWidths(new float[] { 1f, 1f });
		signatures.addCell(signatureCell("Class Teacher"));
		signatures.addCell(signatureCell("Principal"));
		document.add(signatures);
	}

	private PdfPCell signatureCell(String label) {
		PdfPCell cell = new PdfPCell();
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setPaddingTop(30f);
		cell.addElement(new Paragraph("_______________________", SIGNATURE_FONT));
		cell.addElement(new Paragraph(label, LABEL_FONT));
		return cell;
	}

	private void addFooter(Document document) throws DocumentException {
		Paragraph footer = new Paragraph(
				"Generated on " + GENERATED_AT_FORMAT.format(java.time.Instant.now()) + " UTC", FOOTER_FONT);
		footer.setAlignment(Element.ALIGN_RIGHT);
		footer.setSpacingBefore(20f);
		document.add(footer);
	}

	private PdfPTable rule() throws DocumentException {
		PdfPTable rule = new PdfPTable(1);
		rule.setWidthPercentage(100);
		PdfPCell cell = new PdfPCell();
		cell.setBackgroundColor(BRAND_NAVY);
		cell.setFixedHeight(2f);
		cell.setBorder(Rectangle.NO_BORDER);
		rule.addCell(cell);
		return rule;
	}
}
