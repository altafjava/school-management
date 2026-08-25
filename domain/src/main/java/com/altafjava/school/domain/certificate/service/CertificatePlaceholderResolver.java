package com.altafjava.school.domain.certificate.service;

import java.util.Map;

/**
 * Resolves {@code {{placeholder}}} tokens in a certificate body against a value map — the exact
 * same token convention and substitution algorithm platform's
 * {@code NotificationServiceImpl#processTemplate} already uses for {@code NotificationTemplate}
 * bodies, reused here instead of inventing a second templating mechanism. Pure logic, no Spring
 * dependency: a domain service per CLAUDE.md's "domain services: no Spring annotations" rule.
 */
public final class CertificatePlaceholderResolver {

	private CertificatePlaceholderResolver() {
	}

	public static String resolve(String bodyTemplate, Map<String, String> values) {
		if (bodyTemplate == null) {
			return "";
		}
		String result = bodyTemplate;
		for (Map.Entry<String, String> entry : values.entrySet()) {
			String value = entry.getValue() != null ? entry.getValue() : "";
			result = result.replace("{{" + entry.getKey() + "}}", value);
		}
		return result;
	}
}
