package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.guardian.model.GuardianSelfRegistrationMode;

public record UpdateGuardianRegistrationSettingsRequest(@NotNull GuardianSelfRegistrationMode mode) {
}
