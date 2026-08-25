package com.altafjava.school.application.certificate;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.lowagie.text.BadElementException;
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

/**
 * Renders a resolved certificate body (placeholders already substituted) into a branded PDF byte
 * array. Pure formatting — no persistence, no I/O beyond the in-memory buffer (and the
 * caller-supplied logo bytes, already resolved) — mirrors {@code ReportCardPdfGenerator}'s shape
 * and visual system exactly, so both PDF pipelines stay consistent.
 */
@Component
public class CertificatePdfGenerator {

	private static final Color BRAND_NAVY = new Color(30, 58, 95);
	private static final Color BORDER_GRAY = new Color(200, 200, 200);
	private static final Color MUTED_TEXT = new Color(110, 110, 110);

	private static final Font TENANT_NAME_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, BRAND_NAVY);
	private static final Font TITLE_FONT = new Font(Font.HELVETICA, 20, Font.BOLD, BRAND_NAVY);
	private static final Font BODY_FONT = new Font(Font.HELVETICA, 12, Font.NORMAL);
	private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.ITALIC, MUTED_TEXT);

	private static final float LOGO_MAX_HEIGHT = 42f;
	private static final DateTimeFormatter GENERATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
			.withZone(ZoneOffset.UTC);

	public byte[] generate(String certificateName, String resolvedBody, String tenantName, byte[] logoBytes) {
		Document document = new Document(com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(document, out);
			document.open();

			addHeader(document, tenantName, logoBytes);
			document.add(rule());
			document.add(new Paragraph(" "));

			Paragraph title = new Paragraph(certificateName, TITLE_FONT);
			title.setAlignment(Element.ALIGN_CENTER);
			title.setSpacingAfter(20f);
			document.add(title);

			addBody(document, resolvedBody);
			addFooter(document);
		} catch (DocumentException e) {
			throw new BusinessException("Failed to generate certificate PDF: " + e.getMessage());
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
				// A corrupt/unreadable logo must never block certificate generation — fall back to
				// the text-only header exactly as if no logo were configured at all.
				header = new PdfPTable(1);
				header.setWidthPercentage(100);
			}
		}
		PdfPCell nameCell = new PdfPCell(new Paragraph(tenantName, TENANT_NAME_FONT));
		nameCell.setBorder(Rectangle.NO_BORDER);
		nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		header.addCell(nameCell);
		document.add(header);
	}

	private void addBody(Document document, String resolvedBody) throws DocumentException {
		PdfPTable frame = new PdfPTable(1);
		frame.setWidthPercentage(100);
		PdfPCell cell = new PdfPCell(new Paragraph(resolvedBody, BODY_FONT));
		cell.setPadding(20f);
		cell.setBorderColor(BORDER_GRAY);
		frame.addCell(cell);
		document.add(frame);
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
