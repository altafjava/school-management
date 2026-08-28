package com.altafjava.school.domain.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A postal address, embedded directly into {@code Student}/{@code Teacher}/{@code Guardian}.
 * <p>
 * Every component is optional (nullable), including {@link #region} — many countries have no
 * state/province concept, and forcing one on every tenant regardless of country would make the
 * field meaningless noise for those that don't need it. This is distinct from platform-saas's own
 * {@code core.address.Address} (which requires line1/city/postalCode/country and carries no
 * {@code @Pii} marking) for the same reason {@code TenantBillingAddress} is distinct from it: a
 * person's home address needs PII marking a shared, unannotated embeddable can't express, and this
 * domain's address capture is deliberately optional end-to-end, not just individually nullable
 * fields on an otherwise-required object.
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

	@Pii(type = Pii.PiiType.ADDRESS)
	@Column(name = "address_line1", length = 255)
	private String line1;

	@Pii(type = Pii.PiiType.ADDRESS)
	@Column(name = "address_line2", length = 255)
	private String line2;

	@Pii(type = Pii.PiiType.ADDRESS)
	@Column(name = "address_locality", length = 100)
	private String locality;

	// State/province — deliberately optional; many countries have no such subdivision.
	@Column(name = "address_region", length = 100)
	private String region;

	@Column(name = "address_postal_code", length = 20)
	private String postalCode;

	// ISO 3166-1 alpha-2 (e.g. "US", "IN", "FR") — also the default region hint for
	// PhoneNumberValidator when this address and a phone number share the same entity.
	@Column(name = "address_country_code", length = 2)
	private String countryCode;

	// Defensive copy for entities storing a caller-supplied Address into a field — this type is
	// mutable (Hibernate needs setters), so entities must not retain the caller's own reference.
	public static Address copyOf(Address other) {
		if (other == null) {
			return null;
		}
		return Address.builder()
				.line1(other.line1)
				.line2(other.line2)
				.locality(other.locality)
				.region(other.region)
				.postalCode(other.postalCode)
				.countryCode(other.countryCode)
				.build();
	}
}
