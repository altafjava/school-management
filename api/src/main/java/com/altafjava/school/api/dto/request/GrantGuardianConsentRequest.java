package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.guardian.model.GuardianConsentType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for the current guardian to grant consent for a linked student")
public record GrantGuardianConsentRequest(
		@NotNull @Schema(description = "Category of processing being consented to") GuardianConsentType consentType,
		@Schema(description = "Privacy-policy version this consent was given under", example = "2026-01") String policyVersion) {
}
