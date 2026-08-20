package com.altafjava.school.api.dto.response;

// clientSecret is returned only in this direct synchronous response to the caller that just
// created the charge — it is never persisted or exposed by any other endpoint.
public record FeeChargeResponse(
		String gatewayChargeReference,
		String status,
		String clientSecret) {
}
