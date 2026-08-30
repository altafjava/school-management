# FERPA / COPPA / GDPR-K / DPDP-Minors Controls Mapping

Maps US FERPA (Family Educational Rights and Privacy Act) and COPPA (Children's Online Privacy
Protection Act), EU GDPR's "GDPR-K" provisions for children's data, and India's DPDP Act
provisions applicable to minors, to the specific school-saas + platform-saas controls that satisfy
them. Student records are minors' data, which these regimes treat more strictly than adult
personal data — this document is additive to `platform-saas/docs/compliance/SOC2_CONTROLS.md` and
`DPDP_CONTROLS.md`, which cover the generic GDPR/DPDP/SOC2 controls every tenant (school or
otherwise) inherits from the platform.

**Last updated:** 2026-08-30
**Scope:** school-saas only. A future hospital/HR domain project on the same platform would need
its own equivalent document for its own regulatory regime (HIPAA, etc. — see platform-saas
ROADMAP.md Phase F-2).

---

## How to read this document

| Column | Meaning |
|---|---|
| Requirement | The FERPA/COPPA/GDPR-K/DPDP requirement |
| Control | What school-saas (or the platform it runs on) does to satisfy it |
| Evidence | File/class where the control lives |
| Gap | What's missing, if anything, and what would close it |

---

## Parental/guardian consent

| Requirement | Control | Evidence | Gap |
|---|---|---|---|
| A minor's data-processing consent must come from a parent/guardian, not the child (COPPA §312.5; GDPR-K, below the member state's digital-consent age) | `GuardianConsentRecord` — a guardian's own, separately-revocable consent per category (`DATA_PROCESSING`, `DIRECTORY_INFORMATION_DISCLOSURE`, `MARKETING_COMMUNICATIONS`, `PHOTO_VIDEO_MEDIA`), captured self-service by the guardian for a student they are verified-linked to (`GuardianConsentService#grant`, which checks `StudentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId` before allowing it) | `domain/.../guardian/model/GuardianConsentRecord.java`, `application/.../service/GuardianConsentService.java`, `api/.../GuardianController` (`POST/DELETE/GET /api/v1/guardians/me/students/{studentPublicId}/consents`) | Does not verify the guardian's *legal* authority (custody status, age of majority of the student) — it verifies the guardian↔student link a tenant admin already created. Legal-authority verification is an operational/enrollment-process control outside what software can enforce; document this boundary to tenants during onboarding. |
| Consent categories must be separately grantable/revocable, not one blanket flag | `GuardianConsentType` enum (4 categories), one row per (guardian, student, type) | `domain/.../guardian/model/GuardianConsentType.java` | None |
| Distinct from an administrative "we've confirmed this relationship exists" flag | `StudentGuardianLink.consentGivenAt` remains a separate, admin-driven relationship-confirmation flag (`GuardianService#grantConsent`) — not reused for data-processing consent, to avoid conflating the two | `domain/.../guardian/model/StudentGuardianLink.java` | None — this document exists partly to keep the distinction from being re-blurred later |

## Directory information (FERPA §99.31(a)(11))

| Requirement | Control | Evidence | Gap |
|---|---|---|---|
| A school may disclose "directory information" (name, grade, enrollment dates, etc.) unless a parent opts out | `GuardianConsentType.DIRECTORY_INFORMATION_DISCLOSURE` — modeled as an explicit opt-*in* record rather than an opt-out list | `domain/.../guardian/model/GuardianConsentType.java` | FERPA's actual default is opt-out (disclosure allowed unless declined), while this control's default (no `granted` row = not disclosed) is opt-in — stricter than FERPA requires, not a shortfall, but tenants operating under FERPA specifically should be aware the default here is more conservative than the statute mandates. No functional gap. |

## Retention

| Requirement | Control | Evidence | Gap |
|---|---|---|---|
| Personal data must not be kept longer than needed for the purpose it was collected for (GDPR Art.5(1)(e); DPDP §8(7); FERPA does not set a federal retention ceiling but requires *a* documented policy) | `DataRetentionPolicy` (platform, tenant-scoped: `entityType`, `retentionPeriodDays`, `deletionPolicy`) swept daily by `DataRetentionEnforcementScheduler`, delegating to `SchoolDataRetentionHandler` for `entityType=STUDENT` — anonymizes/soft-deletes students who are `WITHDRAWN`/`GRADUATED`/`TRANSFERRED` and past the tenant's configured window, measured from `Student.enrollmentStatusChangedAt` (not `updatedAt`, which an unrelated later edit would also bump) | `platform-saas` `application/.../scheduler/DataRetentionEnforcementScheduler.java`, `application/.../service/CertificateService.java` n/a; `school-saas` `application/.../privacy/SchoolDataRetentionHandler.java`, `/api/v1/data-retention-policies` (platform, tenant-admin CRUD) | The seeded default (7 years post-withdrawal, `SchoolTenantProvisioningListener`) is an engineering placeholder, not a legal opinion — a tenant must confirm the real number for their jurisdiction and update it via `PUT /api/v1/data-retention-policies/{id}` before relying on it. Only `STUDENT` is covered; `Guardian`/`Teacher` retention is not yet modeled (no concrete trigger for it yet — a guardian is an adult, not the class of data these specific regimes single out). |
| A hard-delete option must not be offered where it would corrupt referential integrity | `SchoolDataRetentionHandler` explicitly rejects `HARD_DELETE` for `STUDENT` (throws, logged as a per-policy failure by the scheduler, not silently ignored) — Student rows are referenced by attendance/grades/fee-payment history a hard delete would orphan | `application/.../privacy/SchoolDataRetentionHandler.java` | None — `ANONYMIZE`/`SOFT_DELETE` are the only safe/supported options, documented in the exception message itself |

## Erasure & export (data-subject/parental access rights)

| Requirement | Control | Evidence | Gap |
|---|---|---|---|
| A parent/eligible student may request access to, or erasure of, education records (FERPA §99.10-99.12; GDPR Art.15/17; DPDP §11-12) | Platform's `DataSubjectRequest`/`DataErasureService`/`DataExportService` extension point, with school-saas's `StudentGuardianPiiHandler` (`DomainPiiHandler`) erasing/exporting both `Student` and `Guardian` records for a given platform `userId` | `platform-saas` `application/.../privacy/DataErasureService.java`, `DataExportService.java`; `school-saas` `application/.../privacy/StudentGuardianPiiHandler.java` | Only reachable for a subject who has a linked platform `User` account (`Student.userId`/`Guardian.userId` set) — a student with no login of their own has no direct DSAR path today; in practice their guardian's own DSAR (erasing the guardian's `User`) does not cascade to the linked student record, since erasure is scoped to the requesting `userId` only. A parent requesting erasure *of their child's* record specifically (as opposed to their own account) needs a manual/admin-assisted process today — flagged here rather than silently assumed to work. |

## Breach notification

| Requirement | Control | Evidence | Gap |
|---|---|---|---|
| Certain jurisdictions require notifying affected individuals/regulators after a data breach involving minors' data | None specific to school-saas | — | Deferred, matching platform-saas ROADMAP.md Phase F-2's `BreachNotificationService` (currently HIPAA-scoped and `[ ] Todo`) — no security incident has occurred and no concrete regulatory deadline is driving this; when built, extend it to cover FERPA/COPPA breach-notification timelines, not just HIPAA's, rather than building a second parallel mechanism. |

## Data residency

| Requirement | Control | Evidence | Gap |
|---|---|---|---|
| Some jurisdictions restrict where a minor's data may be physically stored (DPDP cross-border rules; some US state student-privacy laws) | `Tenant.dataResidencyRegion` routes object/file storage (uploads, generated PDFs/report cards/certificates) to a region-specific S3 bucket when one is provisioned | `platform-saas` `infrastructure/.../storage/S3StorageService.java`, `domain/.../storage/model/StorageRegionConfig.java` | Does not relocate the tenant's database — see `platform-saas` ROADMAP.md §H-4 ("Multi-Region Data Residency", `[ ] Todo`, a deliberately deferred major infrastructure undertaking). Do not represent this as full data residency to a tenant/regulator asking specifically about database location. |

---

## Third-party security assessment plan

No independent penetration test or SOC 2 Type I assessment has been performed on school-saas or
the platform it runs on — confirmed by searching both repos for any such record. This is a real,
acknowledged gap (doc22 item 4.5), separate from the automated OWASP Dependency-Check scanning
already running in CI.

- **Readiness evidence that already exists**: `platform-saas/docs/compliance/SOC2_CONTROLS.md` (a
  full Trust Service Criteria mapping with file/class evidence — the raw material a SOC 2 assessor
  would want) and this document. Neither substitutes for an actual independent assessment.
- **Recommended scope for a first engagement**: a web-application penetration test covering the
  authenticated API surface (JWT/MFA/SSO flows, RBAC/tenant-isolation boundaries, the file-upload
  and payment-webhook endpoints), not a full SOC 2 Type II (which requires an observation period
  against a live production deployment that does not yet exist per `platform-saas/ROADMAP.md`
  §8.3). A SOC 2 Type I readiness assessment (a point-in-time control-design review, no observation
  period required) is achievable sooner and is the natural next step once a real production
  deployment exists.
- **Trigger**: before the first enterprise/institutional sale that requires it contractually, or
  before onboarding a tenant in a regulated sector — not on a fixed calendar date. Revisit this
  plan once a sales conversation actually surfaces the requirement, per this codebase's own
  "don't build ahead of a real trigger" philosophy (see `docs/architecture-analysis/21-....md` §6).
- **Owner/timeline**: unassigned — this is a procurement/business decision (selecting and
  contracting a third-party assessor), not something resolvable from the codebase alone. Flagged
  here so it is not rediscovered as a surprise gap during a sales cycle.

## Explicitly out of scope, not silently missing

- **Age verification** — nothing in school-saas verifies a student's actual age; enrollment data (`dateOfBirth`) is trusted as entered by the school/guardian during admission. COPPA's "actual knowledge" standard is generally satisfiable in a B2B-to-schools model (the school, not the platform, is the entity with the direct relationship to the child), but this is a legal characterization question for the operator's counsel, not something this document can settle.
- **Legal-guardianship verification** — see the parental-consent row above.
- **A jurisdiction-by-jurisdiction legal review** — this document maps *technical* controls to *named* requirements; it is not a substitute for the legal/compliance review doc 22 item 4.2 calls for before selling into a specific new jurisdiction.
