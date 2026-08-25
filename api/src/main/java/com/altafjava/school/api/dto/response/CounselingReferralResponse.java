package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;
import com.altafjava.school.domain.counseling.model.CounselingReferralStatus;

public record CounselingReferralResponse(
		String publicId,
		Long studentId,
		Long referredByUserId,
		String reason,
		LocalDateTime referredAt,
		CounselingReferralStatus status,
		Long counselingSessionId) {
}
