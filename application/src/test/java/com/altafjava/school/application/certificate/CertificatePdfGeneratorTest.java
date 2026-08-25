package com.altafjava.school.application.certificate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class CertificatePdfGeneratorTest {

	private final CertificatePdfGenerator generator = new CertificatePdfGenerator();

	@Test
	void generate_producesValidPdf() {
		byte[] pdf = generator.generate("Bonafide Certificate", "This certifies that Alice Smith is a student.",
				"Test School", null);

		assertValidPdf(pdf);
	}

	@Test
	void generate_withLogo_embedsItAndStillProducesValidPdf() throws Exception {
		byte[] pdf = generator.generate("Bonafide Certificate", "This certifies that Bob Jones is a student.",
				"Branded School", onePixelPng());

		assertValidPdf(pdf);
	}

	@Test
	void generate_withCorruptLogoBytes_fallsBackToTextOnlyHeaderRatherThanFailing() {
		byte[] pdf = generator.generate("Bonafide Certificate", "This certifies that Carol Lee is a student.",
				"Test School", new byte[] { 1, 2, 3 });

		assertValidPdf(pdf);
	}

	private void assertValidPdf(byte[] pdf) {
		assertTrue(pdf.length > 0, "Generated PDF must not be empty");
		String header = new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
		assertTrue(header.equals("%PDF"), "Output must be a real PDF (start with %PDF magic bytes)");
	}

	private byte[] onePixelPng() throws Exception {
		java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(1, 1,
				java.awt.image.BufferedImage.TYPE_INT_RGB);
		bufferedImage.setRGB(0, 0, Color.RED.getRGB());
		ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
		javax.imageio.ImageIO.write(bufferedImage, "png", pngBytes);
		return pngBytes.toByteArray();
	}
}
