package com.altafjava.school.api.dto.response;

public record AddressResponse(
		String line1,
		String line2,
		String locality,
		String region,
		String postalCode,
		String countryCode) {
}
