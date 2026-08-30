package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.domain.guardian.model.GuardianConsentType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A guardian's consent record for one category of processing on a linked student")
public record GuardianConsentRecordResponse(
		@Schema(description = "Public UUID of the consent record") String id,
		@Schema(description = "Category of processing this record covers") GuardianConsentType consentType,
		@Schema(description = "Whether consent is currently granted") boolean granted,
		@Schema(description = "When consent was last granted") Instant grantedAt,
		@Schema(description = "When consent was last revoked, if applicable") Instant revokedAt,
		@Schema(description = "Privacy-policy version this consent was given under") String policyVersion) {
}
