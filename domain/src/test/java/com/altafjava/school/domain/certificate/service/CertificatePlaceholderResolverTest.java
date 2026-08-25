package com.altafjava.school.domain.certificate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CertificatePlaceholderResolverTest {

	@Test
	void resolve_withMatchingPlaceholders_substitutesAllTokens() {
		String template = "This certifies that {{studentName}} of {{className}} was admitted on {{admissionDate}}.";
		Map<String, String> values = Map.of(
				"studentName", "Alice Smith",
				"className", "Grade 5 A",
				"admissionDate", "2020-06-01");

		String resolved = CertificatePlaceholderResolver.resolve(template, values);

		assertEquals("This certifies that Alice Smith of Grade 5 A was admitted on 2020-06-01.", resolved);
	}

	@Test
	void resolve_withUnmatchedPlaceholder_leavesTokenLiteral() {
		String template = "Hello {{studentName}}, {{unknownToken}}!";
		Map<String, String> values = Map.of("studentName", "Bob");

		String resolved = CertificatePlaceholderResolver.resolve(template, values);

		assertEquals("Hello Bob, {{unknownToken}}!", resolved);
	}

	@Test
	void resolve_withNullValueForKnownKey_substitutesEmptyString() {
		Map<String, String> values = new HashMap<>();
		values.put("className", null);

		String resolved = CertificatePlaceholderResolver.resolve("Class: {{className}}", values);

		assertEquals("Class: ", resolved);
	}

	@Test
	void resolve_withNullTemplate_returnsEmptyString() {
		assertEquals("", CertificatePlaceholderResolver.resolve(null, Map.of()));
	}
}
