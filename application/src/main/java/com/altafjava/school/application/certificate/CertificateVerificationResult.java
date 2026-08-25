package com.altafjava.school.application.certificate;

import java.time.Instant;

// Minimal, non-sensitive confirmation that a certificate with a given verification code was
// genuinely issued — deliberately excludes the PDF, storage key, and any other student PII beyond
// the name printed on the certificate itself.
public record CertificateVerificationResult(String studentName, String certificateName, Instant issuedAt) {
}
