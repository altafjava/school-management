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
 * Every hand-written {@code @Query} on a repository must reference the tenant — isolation here is
 * 100% application-layer (no DB-level Row-Level Security), so a forgotten tenant predicate is a
 * direct cross-tenant leak with no second layer of defense.
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
