package com.altafjava.school.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import org.springframework.data.jpa.repository.Query;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Every hand-written {@code @Query} (JPQL or native) on a repository must reference the tenant —
 * isolation here is 100% application-layer (Hibernate {@code @Filter} + {@code tenant_id} +
 * {@code TenantContext}, no DB-level Row-Level Security), so a custom query that forgets the
 * tenant predicate is a direct cross-tenant data leak with no second layer of defense to catch it.
 * Starts green (all 47 existing {@code @Query} methods already reference it) — this is a fitness
 * test against regression, not a currently-failing gate.
 * <p>
 * Deliberately narrow: {@code findAll}/{@code findById}/derived-query-method leaks are still
 * possible and unchecked here (Hibernate's {@code @Filter} covers those at the session level
 * instead) — this rule only targets the one place a hand-written query can bypass that filter
 * outright.
 */
@AnalyzeClasses(packages = "com.altafjava.school.domain")
class TenantScopedQueryFitnessTest {

	@ArchTest
	static final ArchRule customQueriesMustReferenceTenant = methods()
			.that().areAnnotatedWith(Query.class)
			.and().areDeclaredInClassesThat().resideInAPackage("..repository..")
			.should(referenceTenant())
			.because("this codebase has no DB-level Row-Level Security — a hand-written @Query "
					+ "that omits the tenant predicate is a direct cross-tenant data leak");

	private static ArchCondition<JavaMethod> referenceTenant() {
		return new ArchCondition<>("reference tenantId or tenant_id in its @Query value") {
			@Override
			public void check(JavaMethod method, ConditionEvents events) {
				Query query = method.reflect().getAnnotation(Query.class);
				String queryText = query.value();
				if (!queryText.contains("tenantId") && !queryText.contains("tenant_id")) {
					events.add(SimpleConditionEvent.violated(method,
							method.getFullName() + "'s @Query does not reference tenantId/tenant_id: "
									+ queryText));
				}
			}
		};
	}
}
