# school-saas — Testing Strategy

**Purpose:** with no frontend built yet, automated tests are the *only* thing standing between a code change and a silent regression. This doc defines the tiers, what belongs in each, coverage/mutation standards with real numbers, and — critically — how CI actually enforces them, since right now it doesn't. Update in place as gates tighten; don't supersede with a new file.

---

## 0. Current baseline

Verified directly against the repo, 2026-08-16 (post-Phase 3 — see `ROADMAP.md` for what changed):

- 366 tests total across all modules, all passing: 79 E2E (all 17 controllers — the 14 from Phase 2 plus `AdmissionController`, the bulk-import endpoint on `StudentController`, and the report-card endpoints on `StudentController` — at the full CLAUDE.md minimum; `TeacherScopedDataAccessE2ETest` additionally exercises the Phase 3 classroom-narrowing fix through the real HTTP/security stack), 68 tenant-isolation/persistence integration tests (including `AdmissionEnrollmentSagaIntegrationTest`, which runs the real saga against a real DB and proves a genuine failure — a colliding student code — leaves no orphaned `Student` and reverts the `Admission` to `APPROVED`, not a mocked assertion that compensation "runs"), 105 `application` unit tests, 28 `domain` unit tests (`AdmissionTest`, `AdmissionDecisionTest`, `ReportCardTest` new this phase) — plus 5 ArchUnit fitness tests, 8 contract tests (OpenAPI snapshot + platform SPI), and the pre-existing validation/migration/scheduler/policy tests.
- `ci.yml` runs `compileJava compileTestJava`, `spotlessCheck` (blocking), `test`, **JaCoCo coverage ratchet** (blocking), **SpotBugs** (blocking), **PIT mutation testing** (blocking), then archives all reports.
- **PIT mutation testing is now run in CI, blocking** (`domain/build.gradle`: `mutationThreshold = 70`, `coverageThreshold = 70`) — scoped to `grade.service`/`grade.model`/`fee.service`, the packages Phase 1 actually put real logic in (97% line coverage, 88% mutation score measured 2026-08-16). Getting this working required two real fixes, both documented inline in `domain/build.gradle`: bumping `pitest-core`/`gradle-pitest-plugin` past their Phase-0-era pins (too old to read Java 25 class files — `IllegalArgumentException: Unsupported class file major version 69`), and running the task with `JAVA_HOME` pointed at a JDK 25 host (the plugin has no toolchain support and its subprocess otherwise inherits whatever JDK launched Gradle, silently reporting 0% coverage against Java-25-compiled classes it can't execute). `ci.yml` adds a scoped `actions/setup-java` step for exactly this before the PIT step. Scope intentionally unchanged again in Phase 3 — the saga orchestration and report-card date-range aggregation both live in `application`, which doesn't have PIT wired up at all.
- **Spotless is checked in CI** — blocking, `./gradlew clean build` fails on a formatting violation.
- **JaCoCo coverage ratchet is now blocking, and was raised in Phase 3, not just held** (`domain` floor 41% → 54%, `application` floor 62% → 74%, both measured — not the 85%/80% aspirational targets from §2's table) — `api` stays report-only per §2 (E2E covers it, not unit tests).
- **SpotBugs zero-findings baseline held through Phase 3** — two new findings investigated: `ReportCardService`'s constructor (`EI_EXPOSE_REP2`, the same detector-noise pattern as Phase 2's `TenantAdminNotifier` finding — documented and excluded), and `BulkImportResponse`/`BulkImportResult` exposing a mutable `List` via their record accessors (`EI_EXPOSE_REP`/`REP2` — this one a real, if minor, gap, fixed at the source with a compact-constructor `List.copyOf`, not suppressed). `config/spotbugs/exclude.xml` now has 2 entries, both investigated and documented inline — still not a blanket category suppression.
- **4 ArchUnit fitness tests** (`app/src/test/java/.../architecture/`): controller authorization, entity leakage, DTO-record, module layering — all blocking, all passing against current code including the new Phase 3 controllers and record DTOs.
- **API-shape drift protection**: `OpenApiSnapshotTest` compares the live OpenAPI spec against a committed baseline (`app/src/test/resources/openapi-snapshot.json`), blocking — regenerated for Phase 3's new endpoints. `SchoolPlatformContractVerificationTest` still covers something different (platform SPI contract compliance, not REST response shape).
- **Two real bugs found and fixed along the way, not originally on this list:**
  - A `StaleObjectStateException` in `AdmissionEnrollmentSaga`: making each saga step commit independently (required for compensation to observe earlier steps — see `ROADMAP.md`'s Phase 3 entry) means the in-memory `Admission` reference goes stale after every `save()`, since Hibernate's `@Version` moves on the DB side but not on a discarded return value. Only visible against a real DB with real transaction boundaries — the unit tests (mocked repositories, no real optimistic locking) could not have caught this, which is exactly why the integration tier exists. Fixed by reassigning `admission = admissionRepository.save(admission)` after every write.
  - `SchoolResourceAccessPolicy.isTeacherAssignedToClassroom` compared a caller's platform `userId` directly against `classroom.classTeacherId` (a `Teacher.id`) — the exact conflation Phase 1 documented and left untouched. The one pre-existing test covering it (`SchoolResourceAccessPolicyIntegrationTest.teacher_canAccessOwnClassroom`) had been asserting the *old, wrong* behavior — passing `teacher.getId()` itself as the caller's id, which only "worked" because of the bug it was inadvertently testing for. Both the production code and the test were fixed together; see `ROADMAP.md`'s Phase 3 entry.
- **A real gap found and deliberately not closed:** `@QuotaCheck(metric = "BULK_IMPORTS")` was wired onto the bulk-import endpoint, then removed after a genuinely failing integration test showed `QuotaService` treats any metric with no configured `UsageMetric` row as limit 0 (blocked), not unlimited — meaning the check would have rejected every tenant, not rate-limited real overuse. The real fix is platform-side (seed a default per-plan limit); tracked as deferred, not silently dropped.

Everything below defines the standard this baseline was brought up to. All of §5's CI enforcement plan is now landed — see that section for what's next.

---

## 1. Test Pyramid — three tiers, each with a job the others don't do

Same three mandatory tiers as `platform-saas` (`.claude/CLAUDE.md` § Testing) — no tier substitutes for another.

### Unit (`{ClassName}Test`, JUnit 5 + Mockito, no Spring context)

Tests domain invariants and service rules in isolation. Concrete school-saas examples, once the corresponding logic exists (`ROADMAP.md` Phase 0/1):

- `GradeCalculationTest`: given marks + a grading scale, the correct letter grade comes out — including boundary values (exactly the cutoff between two grades), not just one happy-path number.
- `FeeBalanceCalculatorTest`: partial payments reduce the outstanding balance correctly; an overpayment doesn't produce a negative-owed edge case silently.
- `AttendanceServiceTest`: duplicate-mark prevention (already exists, needs a test); a status transition (once added) is rejected if invalid.
- `AcademicYearServiceTest`: creating a new current year unsets the previous one — this is the exact bug found in `ROADMAP.md` §2 P0; the fix isn't done until a test proves it.
- `StudentEnrollmentStatusTest`: withdrawal actually transitions status, doesn't just soft-delete.

Never unit test controllers, repositories, or security config — per project convention, those are covered by integration/E2E instead.

### Integration (extend `BaseIntegrationTest`, real DB via Testcontainers, no HTTP)

Tests persistence, queries, transactions, events, cross-tenant isolation. Every module needs a `{Module}TenantIsolationIntegrationTest` — ✅ all 10 modules have one as of Phase 0 (2026-08-16), satisfying `CLAUDE.md`'s hard rule ("every new multi-tenant feature requires a cross-tenant isolation test before merge").

### E2E (extend `BaseRestAssuredTest`, full HTTP + real DB)

Tests the actual contract: status codes, response shape, RBAC, tenant isolation. Per-controller minimum (happy path, unauthenticated → 401, wrong role → 403, tenant isolation) — ✅ met by all 17 controllers as of Phase 3 (the 14 from Phase 2 plus `AdmissionController` and the bulk-import/report-card endpoints added to `StudentController`). `TEACHER`/`PARENT`/`STUDENT` roles already exist (seeded per-tenant by `platform-saas`, confirmed in `ROADMAP.md` §2). Phase 1 implemented the PARENT/STUDENT columns of the matrix below for real (`StudentDataAccessE2ETest`), on student-scoped endpoints — `GET /api/v1/students/{publicId}/grades`, `/attendance`, `/fee-balance`, `/report-cards` — since "view own/child's X" needed dedicated student-scoped endpoints, not a filter on the existing admin-facing list endpoints. Phase 3 closed the TEACHER ⚠️ rows below for real:

| Resource | TENANT_ADMIN | TEACHER | PARENT | STUDENT |
|---|---|---|---|---|
| Mark attendance | ✅ any classroom | ✅ own classroom only | ❌ 403 | ❌ 403 |
| View attendance | ✅ any | ✅ own classroom only (Phase 3) | ✅ own child only | ✅ own only |
| Record grade | ✅ any | ✅ own classroom's exams | ❌ 403 | ❌ 403 |
| View grade | ✅ any | ✅ own classroom's exams only (Phase 3) | ✅ own child only | ✅ own only |
| Record fee payment | ✅ any | ❌ 403 | ❌ 403 | ❌ 403 |
| View fee balance | ✅ any | ❌ 403 | ✅ own child only | ✅ own only |
| Decide admission | ✅ any | ❌ 403 | ❌ 403 | ❌ 403 |
| Generate report card | ✅ any | ❌ 403 | ❌ 403 | ❌ 403 |
| View report card | ✅ any | ✅ any (same guard as grades/attendance) | ✅ own child only | ✅ own only |

**Resolved in Phase 3, not deferred any further:** narrowing TEACHER's grade/attendance *view* to their own classroom only (previously the ⚠️ rows above) — `TeacherClassroomScopeResolver` (`application/security/`) bridges the caller's platform user to their `Teacher` record and then to the classrooms they teach; `GradeService.listGrades`/`AttendanceService.listAttendance` apply that scope for TEACHER, unchanged for TENANT_ADMIN. Verified through the full HTTP/security stack, not just the service layer, by `TeacherScopedDataAccessE2ETest` — two real teacher accounts, one classroom each, each sees only their own classroom's records.

Every row is a real test case, not a generic assertion — "parent can view own child's grade AND cannot view another parent's child's grade" is two separate assertions, and the second one is the one that actually catches a broken tenant/ownership filter.

---

## 2. Coverage standards — real numbers, not 100%

**100% coverage is not the goal and shouldn't be treated as one.** Beyond roughly 80-90% on business-logic-bearing code, additional coverage increasingly tests trivial code (getters, `toString`, framework glue) rather than catching defects — the marginal value drops sharply while the maintenance cost of the tests keeps climbing. Chasing 100% produces tests that assert nothing meaningful just to touch a line, which is worse than not testing that line at all — it creates false confidence, exactly what to avoid given the goal here is "a broken change actually gets caught."

Tiered targets, aligned to where correctness actually matters:

| Layer | Line coverage target | Why |
|---|---|---|
| `domain` (entities with business rules, domain services) | 85%+ | Grade computation, fee balances, state transitions — the highest-consequence logic in the system |
| `application` (services, schedulers) | 80%+ | Orchestration logic, cross-entity validation |
| `api` (controllers, mappers, DTOs) | not directly targeted | Covered by E2E instead — per `CLAUDE.md`, controllers aren't unit tested |
| Overall project | 75-80% | A reasonable "healthy codebase" aggregate once the above holds — not a number to chase directly |

**Mutation testing matters more than the line-coverage number.** A test suite can hit 90% line coverage while asserting almost nothing (`assertDoesNotThrow` everywhere) — mutation testing (PIT, already configured on `domain`) actually checks whether the tests would catch a deliberately introduced bug. The existing `mutationThreshold = 70` on `domain` is a reasonable target for business-logic code specifically — once there's real logic to mutate. Don't raise it further; 70% mutation score on domain logic is already a strong bar.

**The gate strategy: ratchet, don't set-and-forget an aspiration.** `platform-saas` already learned this lesson the hard way — a 70% PIT threshold was configured from the start, CI ran it as `continue-on-error: true`, and real coverage sat at core ~32%/domain ~5% for months with nothing forcing improvement, because the gate was aspirational rather than a floor. Don't repeat that here. Instead:

1. **Phase 0**: add JaCoCo, run it in CI, publish the report — **not blocking yet**, because the current real baseline is near zero and a blocking gate with nothing to compare against just gets bypassed or ignored, same failure mode as platform-saas's mistake in the other direction.
2. **End of Phase 0**: record the actual baseline once cross-entity validation + the correctness fixes land with tests. This number becomes the floor.
3. **From Phase 1 onward**: CI fails if `domain`/`application` coverage drops below the last recorded baseline — a **coverage ratchet**, not a fixed target. This is enforceable from day one because it only requires "don't get worse," which is always achievable, unlike "hit 70%" when today's real number is 5%.
4. **PIT**: wire the already-configured `:domain:pitest` task into CI now, as a blocking gate, once Phase 1's real domain logic (grade calc, fee balance) exists to mutate — before that, running it against near-empty logic classes is measuring nothing.

---

## 3. Architecture fitness tests — add these now, they don't exist yet

`platform-saas` uses ArchUnit specifically to make certain classes of regression impossible to merge, not just unlikely. `school-saas` has zero of these. Add, in Phase 0:

- **`ControllerAuthorizationFitnessTest`**: every method in `api.rest.*` handling an HTTP verb has `@PreAuthorize` (class- or method-level), except an explicit, named allowlist (health checks, etc.). This is the single highest-value fitness test — it's the exact mechanism that would have caught `platform-saas`'s own P0-5 finding (13 controllers with no `@PreAuthorize`) automatically instead of requiring a manual audit to find it.
- **`EntityLeakageFitnessTest`**: no controller method returns a JPA `@Entity` type, including inside `List<>`/`Page<>`/nested generics.
- **`DtoRecordFitnessTest`**: every class in `api.dto.*` is a Java record.
- **`ModuleLayeringFitnessTest`**: `domain` never imports `application`/`api`/`infrastructure`; mirrors the platform's own layering rule, now enforced structurally instead of by convention alone.

Each of these is a few hours of work and then runs forever, for free, on every build — this is precisely the "even by mistake" protection being asked for: a fitness test doesn't care whether a bad change was deliberate or an accident, it fails either way.

---

## 4. API contract drift protection — the no-frontend problem, solved directly

With no UI to notice "this endpoint's response shape quietly changed," add a schema snapshot test: generate the OpenAPI spec (springdoc already produces one) during the test build, diff it against a committed baseline file, and fail if they differ without the baseline being explicitly updated in the same PR. This turns an invisible API change into a visible, reviewable diff — the same safety property a frontend integration test would give, without needing one. Add in Phase 0, alongside the E2E backfill — they cover different failure modes (E2E catches "the endpoint behaves wrong," the snapshot catches "the endpoint's shape changed and nothing downstream would know").

---

## 5. CI enforcement plan — concrete, sequenced

`ci.yml` now runs every gate below, all blocking. Landed in this order (never all at once — each needed a real baseline to be meaningful, same reasoning as §2):

1. ✅ **Phase 0**: `spotlessCheck` — blocking. Done 2026-08-16.
2. ✅ **Phase 0**: ArchUnit fitness tests (§3) — blocking. Done 2026-08-16.
3. ✅ **Phase 0**: OpenAPI snapshot test (§4) — blocking. Done 2026-08-16.
4. ✅ **Phase 0**: JaCoCo report generation — not blocking yet (§2), wired and archived per CI run. Done 2026-08-16.
5. ✅ **Phase 1**: JaCoCo ratchet gate — blocking, floor = the baseline Phase 1's build actually measured (`domain` 31%, `application` 47%), via `jacocoTestCoverageVerification` wired into each module's `check`. Done 2026-08-16.
6. ✅ **Phase 1**: `:domain:pitest` wired into CI, blocking at the existing `mutationThreshold = 70`/`coverageThreshold = 70`, scoped to the grade-calculation/fee-balance packages Phase 1 added real logic to. Required upgrading `pitest-core`/`gradle-pitest-plugin` past their Phase-0 pins (too old for Java 25 class files) and a dedicated JDK 25 `actions/setup-java` step in `ci.yml` (the plugin has no toolchain support — see §0 and `domain/build.gradle` for the full explanation). Done 2026-08-16.
7. ✅ **Phase 1**: spotbugs — added with the same "zero findings baseline, then blocking" approach `platform-saas` used successfully; unlike platform-saas's first run (which found real bugs — a JDBC leak, null-dereference risks), school-saas's first run found 6 findings, all fixed at the source rather than suppressed (immutable-collection defensive copies, one double `SecurityContextHolder` call). `config/spotbugs/exclude.xml` stays empty. Done 2026-08-16.
8. ✅ **Phase 2**: JaCoCo ratchet raised, not just held — `domain` 31% → 41%, `application` 47% → 62%, both re-measured after the new Subject/Period/Timetable/scheduler-job code landed with real tests. Done 2026-08-16.
9. ✅ **Phase 2**: SpotBugs zero-findings baseline held — one genuine-looking finding (`EI_EXPOSE_REP2` on `TenantAdminNotifier`) investigated and confirmed to be detector noise on a pattern used safely elsewhere in the codebase, not suppressed blindly; `config/spotbugs/exclude.xml` gained its first entry, with the investigation documented inline. Done 2026-08-16.
10. ✅ **Phase 2**: OpenAPI snapshot regenerated for 3 new controllers plus the `subjectId` shape change on `ExamController`/`GradeController`. Done 2026-08-16.
11. ✅ **Phase 3**: JaCoCo ratchet raised again — `domain` 41% → 54%, `application` 62% → 74%, re-measured after the Admissions/report-card/bulk-import code landed with real tests. Done 2026-08-16.
12. ✅ **Phase 3**: SpotBugs zero-findings baseline held — two findings investigated (`ReportCardService`'s constructor, excluded as detector noise matching Phase 2's precedent; `BulkImportResponse`/`BulkImportResult`'s exposed mutable `List`, a real gap fixed at the source via a compact-constructor `List.copyOf`, not excluded). Done 2026-08-16.
13. ✅ **Phase 3**: OpenAPI snapshot regenerated for the 3 new endpoint groups (admissions, bulk-import, report-cards). Done 2026-08-16.

This was more aggressive than `platform-saas`'s own history (which started several of these as advisory-only and took longer to catch up) — deliberately, because school-saas was small enough to set each gate correctly from the start instead of retrofitting rigor onto a larger codebase later, which is exactly the more expensive path platform-saas had to pay down.

---

## 6. Definition of done, per PR touching business logic

- [ ] Unit test(s) added for the new/changed rule, including at least one boundary/edge case — not just the happy path.
- [ ] Integration test covers the persistence/query path, including tenant isolation if the entity is tenant-scoped.
- [ ] E2E test covers the endpoint for every actor role that can reach it (§1 RBAC matrix), not just "authenticated user."
- [ ] `./gradlew clean build` green — compile, tests, spotless, ArchUnit fitness tests, OpenAPI snapshot all pass.
- [ ] Coverage on touched `domain`/`application` classes did not drop below the recorded baseline.
- [ ] If the change touches a resource type new to `SchoolResourceAccessPolicy`, the policy and its test are updated together, not left implicit.
