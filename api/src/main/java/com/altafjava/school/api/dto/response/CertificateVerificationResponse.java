package com.altafjava.school.api.dto.response;

import java.time.Instant;

// Deliberately minimal: no publicId, no storage/download reference, no other student PII beyond
// the name printed on the certificate — this is the public verification surface.
public record CertificateVerificationResponse(String studentName, String certificateName, Instant issuedAt) {
}
