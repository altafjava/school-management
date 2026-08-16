# school-saas — Backend Product Architecture & Roadmap

**Scope:** `school-saas` backend only. No frontend content here by design — see `platform-saas/STRATEGIC_ASSESSMENT_AND_ROADMAP.md` at the workspace root for that thread when it's back in scope.
**Method:** grounded in the actual code, verified 2026-08-16 — not the doc-writing "audit" pattern this project has already moved away from (see `platform-saas/.claude/CLAUDE.md` § Documentation Policy). Every claim below cites a real file. Update this doc in place as modules ship; don't supersede it with a new file.

---

## 0. What's actually here today

Confirmed by direct inspection, not by trusting any prior doc:

- **9 domain modules exist as scaffolding, not working features**: `Student`, `Teacher`, `Classroom`, `Attendance`, `AcademicYear`, `Exam`, `Grade`, `FeeStructure`, `FeePayment`. Entities are flat data bags with zero relations (all cross-references are raw `Long` — see §6 for why that's actually the right call). Services do CRUD plus one duplicate-check guard each — no cross-entity validation, no numeric→letter grade computation, no fee balance/outstanding calculation. 5 of 6 scheduler jobs are literal no-op stubs.
- **No Guardian/Parent, Subject, Curriculum, Timetable, Admission, or Enrollment-as-distinct-entity exists anywhere.** Confirmed by grep, not inferred.
- **`platform-saas` already has more than school-saas currently uses.** Multi-campus hierarchy (`Organization` → `Tenant`), feature flags, quota enforcement, an entity-extension pattern, and an opt-in audit aspect are all real, working code — just not yet wired into any school-domain feature. Building school-saas from here means *using* platform capabilities that already exist, not inventing them.

This is the honest starting line the rest of this document builds from.

---

## 1. Complete Backend Feature Set — where each capability belongs

| Capability | Belongs in | Why |
|---|---|---|
| Students, Guardians, Teachers, Staff | `school-saas` | Domain data |
| Academic years, terms, classes/sections, subjects, curriculum | `school-saas` | Domain data |
| Admissions, enrollment | `school-saas` | Domain workflow |
| Attendance, timetable | `school-saas` | Domain workflow |
| Exams, grades, report cards, assignments | `school-saas` | Domain workflow |
| Fee structures, invoices, payments, scholarships/discounts | `school-saas` domain logic **on top of** `platform-saas` billing infra (Stripe integration, invoicing, dunning already exist platform-side) |
| Communication content (what gets sent, to whom, when) | `school-saas` | Domain workflow |
| Notification delivery (email/push/in-app transport) | `platform-saas` (`NotificationService`) — already exists, school-saas just calls it |
| Events (school calendar) | `school-saas` | Domain data |
| Leave management (staff/student) | `school-saas`, P2 |
| Discipline | `school-saas`, P2 |
| Library, transport, inventory/assets | `school-saas`, P2 — genuinely optional modules, build only on real demand |
| Documents/certificates | `school-saas` domain logic **on top of** `platform-saas` `StorageService` (S3/MinIO abstraction already exists) |
| Reporting/analytics | `school-saas` read models **on top of** `platform-saas` observability/metrics infra where it overlaps (e.g. tenant health) |
| Search | `school-saas` indices **on top of** `platform-saas` Elasticsearch infra (tenant-isolated indices, circuit breaker already exist) |
| Import/export | `school-saas` domain-specific format logic; bulk-write mechanics can reuse platform patterns but there's no generic bulk-import framework yet — build the first one in school-saas, extract to platform only if a second domain app needs it (matches how `platform-saas` itself grew: build for real need, generalize on the second consumer) |
| Audit/history | `platform-saas` provides the mechanism (`@Audited` + `AuditAspect`, real but opt-in — see §0); `school-saas` decides *which* mutations need it |
| Multi-tenancy, RBAC infra, feature flags, subscription/billing infra, caching, search infra, observability, file storage | `platform-saas` | Already built, verified real |
| Multi-campus hierarchy | `platform-saas` (`Organization`/`Tenant`) | **Already exists** — see §3, don't rebuild this in school-saas |
| Integrations (SIS export, parent mobile push, third-party gradebook/LMS sync) | External integration, `school-saas`-side adapters later, none now |
| AI-powered capabilities | See §10 — mostly Future |

**What should NOT be built initially:** Library, Transport, Inventory/assets, Discipline, Leave management — real modules, genuinely optional, no evidence of demand yet. A custom RBAC/permission engine, a custom feature-flag system, a custom multi-campus hierarchy, or a custom audit system — all already exist in `platform-saas`; building parallel versions in `school-saas` would be pure waste.

---

## 2. Prioritization

**P0 — Foundation (existing modules must be correct before anything new is built on them)**

- Fix `StudentService.withdraw()` — currently soft-deletes without transitioning `enrollmentStatus` to `WITHDRAWN`; a withdrawn student's status is indistinguishable from an active one that got soft-deleted for another reason.
- Fix `AcademicYearService.create()` — doesn't unset the previous current year when called directly (only the scheduler's rollover job does this correctly); calling the service API directly can produce two "current" years.
- Fix `MarkAttendanceRequest` status parsing — `AttendanceStatus.valueOf()` is called in the controller on unvalidated input; an invalid value throws an unhandled `IllegalArgumentException` instead of a clean 400. Same class of bug is worth auditing across all 9 controllers' enum-typed fields.
- Add application-layer existence validation for every cross-entity `Long` reference (`studentId`, `classroomId`, `examId`, `feeStructureId`, …) before save — right now none of the 9 services check that a referenced ID exists in the tenant. This is a real correctness gap, not a nice-to-have (see §6 for why this belongs at the application layer, not as a JPA `@ManyToOne`).
- Seed `TEACHER`, `PARENT`, `STUDENT` roles (platform's `Roles` class only reserves `SUPER_ADMIN`/`TENANT_ADMIN` — additional roles are meant to be domain data, not platform code; see §3). Every other P0/P1 item assumes these exist.
- Backfill E2E test coverage to the project's own stated minimum (happy path, 401, 403, tenant isolation per controller) — currently 1 of 9 controllers meets it.

**P1 — Core Product**

- ✅ **Done (2026-08-16, Phase 1):** `Guardian`/`Parent` entity + relationship to `Student`. This is the single biggest missing piece — almost every real school workflow (fee responsibility, communication, consent, report access) assumes a guardian exists, and retrofitting a third actor type after building parent-facing features around it is expensive. Model it now, before more is built on `Student` alone.
- ✅ **Done (2026-08-16, Phase 1):** Grade computation: numeric → letter grade, with the grading scale itself tenant-configurable (via `TenantSettingOverride`, already exists platform-side — don't hardcode a single scale).
- ✅ **Done (2026-08-16, Phase 1):** Fee balance / outstanding / partial-payment tracking on `FeePayment`+`FeeStructure` — right now a payment is recorded but nothing ever computes what's owed. A fee module that can't answer "how much does this student still owe" isn't a fee module yet.
- Implement the 5 stub scheduler jobs for real, using the platform's already-working `NotificationService`: attendance reminders, exam reminders, fee reminders, attendance summary reports, report card generation trigger.
- `Subject`/`Curriculum` as first-class entities (currently a free-text string on `Exam`/`Grade`) — needed before Timetable can exist meaningfully.
- Timetable (period × classroom × subject × teacher × time slot).
- Bulk student-roster import (CSV) — high leverage for onboarding a new tenant; a school with 500 existing students isn't going to type them in one by one.
- Report card generation (PDF) — depends on grade computation being real first.

**P2 — Advanced**

- Admissions workflow (application → decision → auto-enrollment → billing-account creation) — a genuine `SagaCoordinator` candidate (the platform's saga infra already exists and is unused by school-saas), since it's a real multi-step process with compensable steps.
- Discipline/behavior tracking, leave management, document/certificate generation beyond report cards, advanced reporting dashboards.
- Library, transport, inventory/assets — build only if a real tenant asks.

**P3 — Premium / Differentiating**

- Organization-level rollup reporting — aggregate attendance/fees/enrollment across a school group's campuses, using the `Organization`→`Tenant` hierarchy that already exists platform-side. This is a genuine, realistic differentiator: most school-management SaaS products are single-campus-per-account; this platform can do cross-campus reporting almost for free because the hierarchy is already built.
- ~~Wiring `Plan.featuresJson`/`limitsJson` to actual feature-gating~~ — done (2026-08-16), see §3.
- Parent-facing API surface hardening (rate limits, dedicated auth scope) for an eventual mobile app.

**Future / Experimental**

- AI-assisted report-card comments, attendance-anomaly detection, teacher assistant features, predictive analytics. See §10 — deliberately not ranked higher.

**Feature flags / subscription plans / tenant config — what goes where, using what already exists:**

- Per-tenant module on/off (e.g. "this tenant has Transport enabled"): `FeatureFlagService.isEnabled(key, tenantId)` — real, working API, use it directly. Don't build a second flag mechanism.
- Usage limits (max students, max staff per tenant): `@QuotaCheck` + `QuotaService` — real, working, annotation-driven. Use it on the relevant create endpoints (`StudentController.enroll`, etc.).
- "What does this subscription plan include": **resolved (2026-08-16)** — `platform-saas`'s `PlanEntitlementSyncService` now parses `Plan.featuresJson`/`limitsJson` and materializes them into `TenantFeatureOverride`/`UsageMetric.limitValue` automatically whenever a subscription is activated (`SubscriptionActivatedEvent`) or its plan changes (`SubscriptionPlanChangedEvent` — enables newly granted features, explicitly revokes features lost in a downgrade, replaces limits with the new plan's values). school-saas never needs to hardcode plan-name checks — call `FeatureFlagService.isEnabled(key, tenantId)` / rely on `QuotaService` as normal; entitlements stay in sync with whatever plan the tenant is actually on.
- Tenant-specific custom fields (a school wants to track something school-saas doesn't model): `EntityAttributeService`'s key/value extension pattern — genuinely useful for low-query-frequency custom fields, but it's untyped and unindexed. Don't reach for it as a substitute for a real column when the field needs to be queried, sorted, or validated.

---

## 3. Multi-Tenant SaaS Architecture

**Tenant isolation** is already a platform-enforced boundary, not something school-saas developers need to remember per query — the triple-strategy multi-tenancy (shared/schema/per-tenant-pool) with Hibernate `@Filter` defense-in-depth is verified real and already exercised correctly by every one of the 9 existing school-saas modules (all extend `TenantAwareEntity`/`SoftDeletableEntity`). Nothing to change here — this is the platform working as designed.

**School/campus hierarchy — already solved, use it, don't rebuild it.** `Organization` (name, slug, contact/billing email, `maxTenants` quota) owns multiple `Tenant`s via `Tenant.organizationId`, with `OrganizationMembership` giving org-level admins visibility across their tenants. Map this directly: **one school group = one Organization; each campus = one Tenant under it.** This answers the "multiple campuses" requirement from the platform layer up — school-saas's job is to build the rollup reporting on top of it (§2, P3), not to invent its own hierarchy.

**RBAC — real gap, needs closing at P0.** Only two roles are wired end-to-end today: `SUPER_ADMIN`, `TENANT_ADMIN`. A `Role` entity and a `Permission` enum (~20 fine-grained values) exist but are completely unreferenced outside their own definitions — no enforcement path uses them. `ResourceAccessPolicy` is the real, working extension point (school-saas already implements it in `SchoolResourceAccessPolicy`, restricting a teacher's classroom `READ` access to their own assignment) — this is the right pattern, extend it, don't replace it. What's missing is the role layer underneath it: seed `TEACHER`, `PARENT`, `STUDENT` as `Role` rows via school-saas's own migration (roles are data, not platform compile-time constants — `Roles.java`'s two constants are the platform's own reserved names, not an exhaustive list), then gate controllers with `@PreAuthorize("hasRole('TEACHER')")` etc., and extend `SchoolResourceAccessPolicy` per new resource type as needed (e.g. a parent can only `READ` their own child's records — **done, Phase 1**: `SchoolResourceAccessPolicy`'s `STUDENT`/`READ` rule, enforced via `StudentDataAccessGuard` on the grades/attendance/fee-balance endpoints).

**Feature flags, entitlements, audit, caching, background jobs, notifications, search** — all real platform capabilities; school-saas's job is to *call* them correctly, not build parallel versions. See §2 for the flag/quota/plan-gating specifics and §0/§8 for the audit-trail specifics.

---

## 4. Domain Architecture — Module Boundaries

**Not just 9 modules.** Today's 9 are a subset. Near-term target is ~10 core modules (table below); with P2/P3 modules (Discipline, Leave, Library, Transport, Inventory — §2) added only on real demand, the eventual count is 15+. The 9 existing ones aren't wrong, they're incomplete — no module here needs to be deleted, several new ones need to be added alongside them.

Keep the modular monolith the codebase already uses (`domain/{context}/model`, `application/{context}/service`, `api/controller` — this is already the actual layout, not a proposal). No microservices split — nothing here has an independent scaling or team-ownership justification yet, and splitting now would be pure speculation against a future that isn't here.

| Module | Responsibility | Owns | Depends on (read-only, via ID) | Key events |
|---|---|---|---|---|
| **Students** | Student identity, enrollment status | `Student` | — | `StudentEnrolledEvent`, `StudentWithdrawnEvent` |
| **Guardians** ✅ *(built, Phase 1)* | Guardian identity, guardian↔student links, consent | `Guardian`, `StudentGuardianLink` | Student (by ID) | `GuardianLinkedEvent` |
| **Staff** | Teacher/staff identity | `Teacher` | — | — |
| **Academics** | Academic years, terms/semesters, classes/sections, subjects, curriculum | `AcademicYear`, `Term` *(new)*, `Classroom`, `Subject` *(new)* | Teacher (by ID, for class-teacher assignment) | `AcademicYearRolledOverEvent` |
| **Admissions** *(new, P2)* | Application intake → decision → enrollment handoff | `Admission`, `AdmissionDecision` | Student (creates one on approval) | `AdmissionApprovedEvent` |
| **Attendance** | Daily attendance records | `Attendance` | Student, Classroom (by ID) | `AttendanceMarkedEvent` |
| **Timetable** *(new, P1)* | Period × classroom × subject × teacher scheduling | `Period`, `TimetableEntry` | Classroom, Subject, Teacher (by ID) | — |
| **Examinations** | Exams, grades, report cards | `Exam`, `Grade`, `ReportCard` *(new)* | Student, Classroom, Subject, Term (by ID) | `GradeRecordedEvent`, `ReportCardGeneratedEvent` |
| **Fees** | Fee structures, payments, balances, scholarships | `FeeStructure`, `FeePayment`, `Scholarship` *(new, P2)* | Student, Guardian, Term (by ID, billing responsibility/cycle) | `FeePaymentRecordedEvent`, `FeeOverdueEvent` |
| **Communication** *(new, P1-P2)* | What gets sent to whom, when — templates and triggers | `CommunicationTemplate`, `CommunicationLog` | Student, Guardian, Teacher (by ID); calls platform `NotificationService` for delivery | — |

**New gap found in this pass: `Term`/`Semester`.** `AcademicYear` currently has no sub-division — real schools run fee cycles, report cards, and often exams per term/semester, not per year. Add `Term` (belongs to one `AcademicYear`, has its own start/end) as part of the Academics module in Phase 1, before Fees/Examinations need a cycle boundary to attach to.

**Two design decisions worth stating explicitly, not leaving implicit** (given the instinct to distrust the current design — these were deliberate, re-examined now, and confirmed rather than overlooked):

- **`Classroom` stays as one entity with `grade`/`section` string fields, not split into separate `Grade`/`Section` entities — for now.** Splitting is the right move only once a section needs its own capacity/roster/teacher-assignment lifecycle independent of its classroom (e.g., multiple sections per grade with different capacities and class teachers, which today's model can't express well). No evidence of that need yet. If/when it arrives, do it then — the fields are contained enough that this isn't a costly deferral, and building it now against no real requirement would be exactly the speculative over-building the brief warns against.
- **Cross-module references stay raw `Long` IDs, not JPA `@ManyToOne`** (see §6) — this is the correct DDD module-boundary pattern, confirmed by platform-saas's own convention (114+9 entities, only one `@OneToMany`/`@ManyToMany` in the entire platform). The gap isn't the reference style, it's that nothing validates the ID exists (§2 P0) — fixed at the application layer, not by changing the reference style.

Every cross-module reference is by ID. Events cross module boundaries; direct entity/repository access across modules does not.

---

## 5. Platform vs School Responsibilities — corrections found

Reviewing the actual code against this split surfaced two things already in the wrong place, and one thing correctly placed that's worth calling out so it isn't "fixed" by mistake:

- **Correctly placed, don't touch:** `SchoolResourceAccessPolicy` living in school-saas, implementing platform's `ResourceAccessPolicy` SPI. This is the extension point working exactly as designed — resist any urge to move classroom-level access logic into the platform.
- **~~Gap in platform-saas~~ — resolved (2026-08-16):** `Plan.featuresJson`/`limitsJson` are now wired end-to-end via `PlanEntitlementSyncService` (see §2). No school-saas workaround needed.
- **Gap in platform-saas, not urgent:** RBAC's `Permission` enum and `Role.permissionsJson` are modeled but never enforced anywhere. If school-saas eventually needs finer-grained permissions than role-based `@PreAuthorize` (e.g. "can view grades but not edit them"), that enforcement plumbing needs to be built in `platform-saas` (it's the natural home for a permission-check mechanism), with school-saas only defining which permissions its roles carry.

---

## 6. Database & Data Architecture

**Tenant strategy:** stay on `SHARED` (row-level `tenant_id`) for school-saas tenants — nothing about school data volume or isolation requirements justifies `SCHEMA` per-tenant at this stage, and the platform's Hibernate `@Filter` defense-in-depth already makes `SHARED` genuinely safe. Revisit only if a specific enterprise customer contractually requires physical schema isolation.

**The "raw `Long` instead of `@ManyToOne`" pattern across all 9 entities is correct, not a shortcut to fix.** It's the right way to reference across aggregate/module boundaries in a modular monolith — it's what keeps modules genuinely separable later if that's ever justified, and it's consistent with platform-saas's own module-boundary discipline. **The actual gap is that nothing validates the referenced ID exists before save** (§2, P0) — that's an application-layer job (a lookup call to the owning module's repository/service), not a reason to introduce JPA object references.

**Indexes:** every new table needs `tenant_id`, `status`/`deleted`, `created_at`, and all FK-shaped `Long` columns indexed — matches `platform-saas`'s own hard rule and its own hard-won lesson (the platform's `PLATFORM_READINESS_ASSESSMENT` audit found and fixed exactly this class of gap on platform tables; don't repeat it here).

**Historical academic data / archiving / partitioning:** not yet. A school's attendance+grade history for even a few thousand students over several years is small by any modern database's standards — partitioning is a technique for a problem this system doesn't have yet. Revisit only when a real tenant's data volume or a real reporting-latency complaint provides the trigger, not preemptively.

**Bulk operations:** the one place this matters now is student-roster import (§2, P1) — design that as a real bulk-insert path (batched, validated, reported per-row failure) from the start, since retrofitting bulk semantics onto a one-row-at-a-time API later is genuinely painful.

---

## 7. Performance & Scalability

Nothing here requires a decision "now to avoid an expensive rewrite later" beyond what's already covered: keep FK-by-ID (§6), keep indexes disciplined (§6), use the platform's paginated list-endpoint convention (already followed by all 9 controllers), use the platform's bounded async executor for non-critical writes (notifications, audit — already the platform default). At current and realistically near-term scale (a handful of tenants, thousands of students each), the platform's existing caching (Caffeine + Redis), connection pooling, and circuit breakers are more than sufficient. Horizontal scaling of the application tier is a deployment-pipeline concern (there isn't one yet — see the root `STRATEGIC_ASSESSMENT_AND_ROADMAP.md`), not a school-saas code concern.

---

## 8. Security & Reliability

- **PII**: `@Pii` is already used on `Student.email`/`Teacher.email` — extend it to `Guardian` fields once that entity exists (phone, address), and audit whether `dateOfBirth` should carry it too.
- **Audit trail — concrete task list, since it's opt-in not automatic (§0):** annotate with `@Audited`: grade changes, fee payment recording, attendance corrections (not the initial mark — the correction), enrollment status transitions. These are the actions a school will actually need to answer "who changed this and when" about.
- **Idempotency:** `FeePaymentService.record()` currently only blocks a duplicate receipt number — tighten this to a real idempotency-key pattern (the platform already has one, per its own README) once payments carry real money-movement consequences, not just record-keeping.
- **Rate limiting, retries, timeouts, circuit breakers:** inherited from the platform for anything going through its HTTP/async infra — nothing school-saas-specific needed here yet.
- **Backup/recovery:** genuinely not applicable yet — there's no production deployment (see root roadmap). Don't write a school-saas-specific DR doc before platform-saas has a real one to inherit from.

---

## 9. API & Integration Architecture

Already-compliant patterns to keep: DTO records (never JPA entities returned), pagination on every list endpoint, `@Valid` request DTOs, `publicId` exposure. Two concrete additions:

- **Bulk import API** for student rosters (§2/§6) — the first real bulk endpoint in school-saas; design its response shape (per-row success/failure) once and reuse the pattern for future bulk needs (bulk grade entry, bulk fee-structure assignment) rather than inventing a new shape each time.
- **Webhooks** — not yet. No external consumer exists to deliver to. Design the outbound event shape (already Java records, IDs/primitives only per platform convention) so adding webhook delivery later is additive, not a rework.

Public vs internal API distinction isn't a real question yet — everything is internal to the one deployed app; revisit when/if a public integration API is actually requested.

---

## 10. Automation & AI — mostly Future, deliberately

Per the platform's own stated principle, don't add AI because it's fashionable. Ranked honestly:

- **Postpone entirely for now:** attendance-anomaly detection, predictive analytics, intelligent search, teacher assistant features. No workflow they'd sit on top of is real yet (§0) — there's nothing to be intelligent about until grades/attendance/fees are genuinely computed, not stubbed.
- **Design for later, don't build now:** automated communication drafting (e.g. a suggested report-card comment from a grade trend) — plausible once report cards (P1) and the Communication module (P1/P2) are both real; not before.
- **Build now:** nothing. This is the correct answer at this stage, not a gap.

---

## 11. Engineering Principles

Inherit `platform-saas`'s `.claude/CLAUDE.md` in full — same group of standards already governs school-saas (module layering, service-layer rules, testing tiers, security rules, hard rules). Additions specific to what this review surfaced:

- **Model actor types (Guardian, Teacher, Student-as-user) as first-class from the start** — retrofitting a third actor type after building features around a two-actor assumption is the single most expensive mistake available here; §2 P1 exists specifically to avoid it.
- **Cross-module references by ID, always** (§6) — already the pattern, keep it as modules grow.
- **Application-layer existence validation is not optional** (§2 P0) — a flat, FK-by-ID design only stays safe if every write path checks what it's referencing.
- **Use platform capabilities before building parallel ones** — feature flags, quotas, audit, entity extension, saga coordination, resource-access policy all already exist; the temptation to build a school-saas-local version of any of these should be treated as a bug in the plan, not a reasonable choice.

**Actively avoid:** premature microservices split, a school-saas-local RBAC/flag/audit system, God services (keep the one-guard-clause-per-service discipline as real logic gets added — split before a service exceeds ~5 responsibilities), building Library/Transport/Inventory/Discipline before real demand, a saga for anything that isn't genuinely multi-step-with-compensation (Admissions qualifies; most CRUD does not), scattering `FeatureFlagService.isEnabled()` calls through business logic instead of checking once at the boundary (controller or a dedicated gating service).

---

## 12. Competitive Differentiation — realistic for a solo developer

- **Multi-campus rollup reporting** (§2 P3) — the platform's `Organization` hierarchy makes this close to free to build here and genuinely rare in typical school-management SaaS, which is usually single-campus-per-account.
- **API-first design** — already true by construction (versioned REST API, OpenAPI/springdoc live, DTO contracts stable) — worth stating as a real strength when it eventually matters for third-party integrations (LMS, SIS sync), not a thing to build, a thing to not accidentally break.
- **Configurability without redeploy** — feature flags + tenant settings + entity extension already give school-saas a real edge over school ERPs that require a vendor ticket to change a grading scale or add a custom field.
- **Not now:** AI-driven differentiation (§10) — real, but premature; the honest differentiator today is architectural soundness, not intelligence features nothing is built to receive yet.

---

## Development Roadmap

Testing/CI gates below are defined in detail in `TESTING.md` — this section states which gate must be real by the end of each phase, not how it works.

**Phase 0 — Harden what exists — ✅ COMPLETE (2026-08-16).** `./gradlew clean build` green: 236/236 tests, `spotlessCheck` blocking, all 4 ArchUnit fitness tests blocking, OpenAPI snapshot test blocking, JaCoCo reporting live per module.

- ✅ All 3 correctness bugs fixed: `StudentService.withdraw()` now transitions `enrollmentStatus` to `WITHDRAWN`; `AcademicYearService.create()` now unsets the previous current year; `MarkAttendanceRequest.status` is a real `AttendanceStatus` enum (was `String` + unchecked `.valueOf()` — an invalid value now gets a clean 400 via Jackson/`HttpMessageNotReadableException`, not an unhandled 500).
- ✅ Cross-entity existence validation added to the 5 services that reference another module's entity by ID (Classroom→Teacher, Attendance→Student/Classroom, Exam→Classroom, Grade→Student/Exam, FeePayment→Student/FeeStructure) — each throws `ResourceNotFoundException` before the write, rather than letting the database's existing FK constraint reject it with a raw `DataIntegrityViolationException`. The FK constraints themselves were already there (real referential integrity was never actually at risk) — this closes the error-experience gap, not a data-corruption one.
- ✅ **TEACHER/PARENT/STUDENT roles were already seeded** — found already present in `platform-saas`'s own `data/seed-tenant-roles.xml` (per-tenant, alongside `TENANT_ADMIN`/`PRINCIPAL`), confirmed by direct inspection. No school-saas migration needed; this line item is satisfied by platform-side work already done.
- ✅ `Term` module built as a full vertical slice (entity, repository, service, controller, DTOs, mapper, Liquibase migration `003-terms.xml`), matching every existing module's pattern exactly.
- ✅ Tenant-isolation integration tests added for all 8 modules that lacked them (Teacher, Classroom, AcademicYear, Exam, Grade, FeeStructure, FeePayment, Term) — 39 integration tests total, up from 2.
- ✅ E2E tests added for all 9 controllers that lacked them (the 8 above + Attendance) — 41 E2E tests total, up from 4, each covering the full CLAUDE.md minimum (happy path, unauthenticated → 401, wrong role → 403, cross-tenant → 404).
- ✅ Unit tests added for all 7 changed services + `TermService` — 25 unit tests, up from 0 (there was no domain-logic unit test coverage in school-saas before this phase).

**Two real bugs found and fixed along the way, neither originally on this list:**
- **`SchoolTenantProvisioningListener` silently failed to seed every new tenant's default academic year.** Bare `@Async` (not `@Async("platformTaskExecutor")`) ran on an executor with no tenant-context-propagating `TaskDecorator`, and — more fundamentally — there was no tenant context to propagate anyway, since a brand-new tenant is never "current" on any request thread. Fixed by running the handler body through `TenantContext.runAsTenant(...)`, the platform's own sanctioned API for exactly this case (see `AbstractBaseJob`'s use of it), binding the tenant from the event payload instead of relying on ambient context. Also switched `@TransactionalEventListener(AFTER_COMMIT)` → plain `@EventListener`, matching platform-saas's own documented reasoning for `TenantCreatedEvent` specifically (outbox-routed, so a `@TransactionalEventListener` isn't guaranteed to fire).
- **A real client can never populate `classTeacherId`/`classroomId`/`studentId`/`examId`/`feeStructureId` on any create request.** Response DTOs correctly expose only `publicId` (per CLAUDE.md's DTO rule), but the corresponding request DTOs across 5 modules require the raw internal `Long` ID — which no GET endpoint ever returns. Not fixed in this phase (would mean touching 5 modules' request DTOs, services, and controllers, and Phase 0's own scope was already large) — worked around in the new E2E tests via direct repository lookup, with the gap documented in each test's class Javadoc. **Flagging this as a real, not-yet-scheduled fix**: request DTOs for cross-entity references should accept the referenced entity's public UUID and resolve it to the internal ID server-side, the same direction the response side already went.

**Phase 1 — Make the core academic + fee workflow real — ✅ COMPLETE (2026-08-16).** Guardian entity + student links, grade computation with tenant-configurable scale, fee balance/partial-payment tracking.

- ✅ `Guardian` + `StudentGuardianLink` entities (`domain/guardian/`), Liquibase `004-guardians.xml`; `guardians.user_id`/`students.user_id` nullable FKs to platform `users` — a guardian or student may optionally hold a login account with role `PARENT`/`STUDENT`. `GuardianService`: create, link to student (`RelationshipType`: MOTHER/FATHER/LEGAL_GUARDIAN/OTHER, `primaryContact`, `consentGivenAt`), publishes `GuardianLinkedEvent`. `GuardianController`: full CRUD + `POST /api/v1/guardians/{publicId}/students` (link) + `GET /api/v1/guardians/me/students` (parent self-service, returns real `StudentResponse`s with `publicId` — not the raw internal ID gap flagged in Phase 0).
- ✅ Grade computation is real: `GradeCalculator` (pure domain service, `domain/grade/service/`) converts marks + `Exam.maxMarks` into a letter grade against a `GradingScale` — no longer client-supplied (`RecordGradeRequest.gradeLetter` removed). `GradingScale`/`GradeThreshold` are tenant-configurable via `GradingScaleService`, which bridges to the platform's `TenantSettingOverrideService` (JSON under key `school.grading.scale`); tenants without an override get `GradingScale.defaultScale()` (A/B/C/D/F at 90/80/70/60/0). `GradingScaleController`: `GET`/`PUT /api/v1/grading-scale` (TENANT_ADMIN).
- ✅ Fee balance is real: `FeeBalanceCalculator` (pure domain service, `domain/fee/service/`) computes `amountDue`/`amountPaid`/`outstandingBalance`/`overpaidAmount` per fee structure from the sum of a student's payments — overpayment is reported explicitly, never silently negative. Every `FeeStructure` in a tenant applies to every student (documented assumption — no fee-structure-to-student assignment table exists yet); narrowing this later doesn't change the calculation itself. Exposed at `GET /api/v1/students/{publicId}/fee-balance`.
- ✅ Ownership-based RBAC, not just role-based: `SchoolResourceAccessPolicy` gained a `STUDENT`/`READ` rule (self or linked guardian, resolved via real `users.id` — not the classroom policy's pre-existing teacher-ID/user-ID conflation, which Phase 1 left untouched as out of scope). `StudentDataAccessGuard` (`application/security/`) enforces it on three new nested read endpoints — `GET /api/v1/students/{publicId}/grades`, `/attendance`, `/fee-balance` — TENANT_ADMIN/TEACHER bypass, PARENT/STUDENT must own the record or get a real 403 (`AccessDeniedException`, not the `ResourceAccessPolicyEnforcer.assertAllowed` 400 path, which doesn't match Spring Security's expected semantics for a denial). `fee-balance` excludes TEACHER entirely, matching TESTING.md's RBAC matrix (§1) exactly.
- ✅ **Testing/CI work (`TESTING.md` §5, steps 5–7) — all landed, all blocking, no aspirational thresholds:**
  - JaCoCo coverage ratchet: `domain` floor 31%, `application` floor 47% (both measured, not guessed) via `jacocoTestCoverageVerification` wired into `check`.
  - `:domain:pitest` wired into CI and blocking at the existing `mutationThreshold = 70`/`coverageThreshold = 70` — required bumping `pitest-core` to 1.25.9 and the `gradle-pitest-plugin` to 1.19.0 (the versions pinned since Phase 0 predate Java 25 / ASM 9.8 support and crashed outright reading this project's class files) and narrowing `targetClasses` from the whole `domain` package down to `grade.service`/`grade.model` (`GradingScale`/`GradeThreshold` only, not the `Grade` entity)/`fee.service` — the packages Phase 1 actually put real logic in. Real result: 97% line coverage, 88% mutation score on that scope. A genuine, separate tooling gap surfaced and is documented in `domain/build.gradle`: the plugin has no Java toolchain support, so its subprocess needs the *Gradle daemon itself* on JDK 25, not just the project's toolchain — `ci.yml` handles this with a scoped `actions/setup-java` step immediately before the PIT step.
  - SpotBugs added — zero-findings baseline achieved by fixing all 6 real findings at the source (immutable list defensive copies on `GradingScale`/`UpdateGradingScaleRequest`/`GradingScaleResponse`, a double `SecurityContextHolder.getAuthentication()` call in `GuardianService`), not by suppressing them; `config/spotbugs/exclude.xml` stays empty.
  - Full RBAC E2E matrix (§1 table): `StudentDataAccessE2ETest` covers TENANT_ADMIN (any student), PARENT (own child ✅ / other child ❌ 403), STUDENT (own ✅ / other ❌ 403), TEACHER excluded from fee-balance — using real platform `User` rows (not the synthetic `user_id=-1` token used elsewhere for plain role checks), since ownership assertions need a real guardian/student-to-user link to prove anything.
- **Dependencies:** Phase 0 (roles must exist before guardian-scoped access control makes sense) — met.
- **Outcome:** a parent (once a frontend exists) can genuinely see their child's real grade and real fee balance — the numbers are correct, not placeholder.
- **Validated:** `GuardianTenantIsolationIntegrationTest` + `SchoolResourceAccessPolicyIntegrationTest` prove a guardian can only see their own linked student's data (and not via any listing endpoint either); PIT mutation score on `GradeCalculator`/`FeeBalanceCalculator` (88%) meets threshold, not just line coverage.
- **Deferred, not silently dropped:** teacher-scoped "own classroom only" filtering on grade/attendance view (TESTING.md's matrix column) — TEACHER keeps the tenant-wide read access it already had from Phase 0; narrowing it to a teacher's own classroom requires walking the Grade→Exam→Classroom / Attendance→Classroom chain and re-touching existing, already-tested endpoints, which was judged a larger, separate change from Guardian/grade-computation/fee-balance. Tracked for Phase 2.

**Phase 2 — Make scheduling and notifications real (~3–4 weeks).** Implement the 5 stub scheduler jobs for real against `NotificationService`. Subject/Curriculum as first-class entities. Timetable module. **Dependencies:** Phase 1 (notification content needs real grade/fee/attendance data to reference). **Outcome:** the platform's scheduling and notification infrastructure is genuinely exercised by school-domain logic for the first time, not just proven to boot at Spring context startup. **Validate:** a scheduled job actually sends a real notification in a test environment and it's verifiably correct content; coverage ratchet held or raised, not just maintained by omission.

**Phase 3 — Onboarding and output (~4–6 weeks).** Admissions workflow (as a saga), bulk student-roster import, report card PDF generation. **Dependencies:** Phase 1 (grades must be real before a report card means anything), Phase 2 (admissions decisions should trigger real notifications). **Outcome:** a new tenant can onboard an existing student body in bulk, run an admissions cycle for new students, and produce a real report card. **This is the first production-ready milestone** — a school could plausibly run a real term on this system, with a CI pipeline that would actually catch a regression in any of it.

**Phase 4 — Multi-campus (~2–3 weeks, can run partly in parallel with Phase 3).** Organization-level rollup reporting API. (The `Plan.featuresJson`/`limitsJson` entitlement wiring originally scoped here was completed early, 2026-08-16 — see §2/§3; nothing left to do for it in this phase.) **Dependencies:** Phase 3 for meaningful data to roll up. **Outcome:** the platform's most distinctive already-built capability (multi-campus) is actually usable, not just structurally present.

**Phase 5+ — P2 modules, on demand only.** Discipline, leave management, library, transport, inventory. No fixed timeline — triggered by a real tenant request, not this roadmap.

---

## Final Recommendation

1. **Build first:** ~~Phase 0's correctness fixes~~ — done (2026-08-16). ~~Phase 1 (Guardian, real grade/fee logic)~~ — done (2026-08-16), see §2/§7 and Development Roadmap above. Build Phase 2 (real scheduling/notifications, Subject/Curriculum, Timetable) next.
2. **Platform capabilities to finish before certain school features:** none block Phase 0–3. The one real platform gap identified in this review (`Plan.featuresJson`/`limitsJson` wiring) has since been closed (2026-08-16) — see §2/§3.
3. **P0/P1/P2/P3 mapping:** see §2 — P0 is entirely correctness fixes on what exists; P1 is Guardian + real grades/fees + real scheduling; P2 is Admissions plus the genuinely-optional modules; P3 is the multi-campus differentiator.
4. **Platform vs school split:** see §1 and §5 — the split is already mostly right; the only corrections found are two unwired platform capabilities (entitlements, fine-grained permissions), not misplaced code.
5. **Feature flags/plans:** use `FeatureFlagService` and `@QuotaCheck` now, as they exist; don't wait for plan-entitlement wiring to use them.
6. **Architecture to commit to:** modular monolith, cross-module references by ID, events across module boundaries — all already the pattern, just keep it disciplined as new modules are added.
7. **Decisions to make now:** model Guardian as first-class before building more on Student alone; keep FK-by-`Long` (don't introduce `@ManyToOne`) but close the validation gap it currently has.
8. **Deliberately postpone:** Library, Transport, Inventory, Discipline, Leave, AI features, webhooks, public API surface, schema-per-tenant, partitioning, microservices.
9. **First production-ready milestone:** end of Phase 3 — real grades, real fees, real notifications, bulk onboarding, admissions, report cards, all correctness-hardened with proper RBAC.
10. **Avoid at this stage:** everything in §11's "actively avoid" list, and building any P2/P3 module speculatively ahead of the phase sequence above.

**Where I'd push back if you were heading elsewhere:** don't be tempted to build Guardian as a bolt-on later once Student-only workflows are further along — it's cheap now (Phase 1) and expensive after more features assume a two-actor world. Phase 0 closed the structural-correctness-vs-functional-completeness gap (correctness bugs, cross-entity validation, test coverage) on the original 9 modules + `Term` — the modules themselves still don't compute a real letter grade or a real fee balance yet (that's Phase 1's job), so don't read Phase 0's green build as "the domain logic is done." It isn't — it's now provably correct at the layer it operates on, which is a different, narrower claim.

**On "delete and rebuild the whole thing":** don't. The module skeleton is sound by direct inspection — the actual gap is that nothing currently enforces it stays that way (no ArchUnit fitness tests, no coverage gate, no mutation testing running in CI despite being configured). See `TESTING.md` — that's the fix for the "feels like a demo" instinct, not a rewrite.
