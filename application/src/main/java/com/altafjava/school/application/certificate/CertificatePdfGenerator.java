package com.altafjava.school.application.certificate;

import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

// Renders a resolved certificate body (placeholders already substituted) into a PDF byte array.
// Pure formatting — no persistence, no I/O beyond the in-memory buffer — mirrors
// ReportCardPdfGenerator's shape so both PDF pipelines stay consistent.
@Component
public class CertificatePdfGenerator {

	private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
	private static final Font BODY_FONT = new Font(Font.HELVETICA, 12, Font.NORMAL);

	public byte[] generate(String certificateName, String resolvedBody) {
		Document document = new Document();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(document, out);
			document.open();

			document.add(new Paragraph(certificateName, TITLE_FONT));
			document.add(new Paragraph(" "));
			document.add(new Paragraph(resolvedBody, BODY_FONT));
		} catch (DocumentException e) {
			throw new BusinessException("Failed to generate certificate PDF: " + e.getMessage());
		} finally {
			document.close();
		}
		return out.toByteArray();
	}
}
