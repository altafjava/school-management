package com.altafjava.school.domain.common.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Country-aware phone validation via Google's libphonenumber — replaces the hardcoded,
 * single-region regex a phone field would otherwise need, since no one pattern covers every
 * country's numbering plan.
 * <p>
 * {@code defaultRegion} is an ISO 3166-1 alpha-2 code (e.g. the phone's owning entity's
 * {@code Address.countryCode}, when set) used only when {@code phone} doesn't already start with
 * a {@code +} country-calling-code prefix; a number already in international format validates
 * without needing one.
 */
public class PhoneNumberValidator {

	private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

	/** Blank input is treated as valid — phone is an optional field, nothing to validate. */
	public boolean isValid(String phone, String defaultRegion) {
		if (phone == null || phone.isBlank()) {
			return true;
		}
		try {
			PhoneNumber parsed = phoneNumberUtil.parse(phone, defaultRegion);
			return phoneNumberUtil.isValidNumber(parsed);
		} catch (NumberParseException e) {
			return false;
		}
	}
}
