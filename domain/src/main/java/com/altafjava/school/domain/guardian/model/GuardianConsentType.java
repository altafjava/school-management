package com.altafjava.school.domain.guardian.model;

/**
 * Categories of processing a guardian consents to on behalf of a linked minor student —
 * FERPA/COPPA/GDPR-K/DPDP require these to be separately grantable and revocable, not a single
 * blanket flag. Distinct from {@link StudentGuardianLink#getConsentGivenAt()}, which confirms the
 * guardian↔student relationship itself, not consent to any specific processing.
 */
public enum GuardianConsentType {
	DATA_PROCESSING, DIRECTORY_INFORMATION_DISCLOSURE, MARKETING_COMMUNICATIONS, PHOTO_VIDEO_MEDIA
}
