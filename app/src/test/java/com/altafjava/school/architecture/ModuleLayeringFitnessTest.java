package com.altafjava.school.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Structurally enforces CLAUDE.md's module-dependency rule
 * ({@code core <- domain <- application <- api}) so a future edit can't reintroduce a
 * cross-layer import by accident — matching platform-saas's own verified-by-import-grep
 * discipline, now checked automatically instead of only by manual audit.
 */
@AnalyzeClasses(packages = "com.altafjava.school")
class ModuleLayeringFitnessTest {

	@ArchTest
	static final ArchRule domainMustNotDependOnApplication = noClasses()
			.that().resideInAPackage("com.altafjava.school.domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"com.altafjava.school.application..",
					"com.altafjava.school.api..")
			.because("domain is the innermost school-owned layer — it must never import application or api");

	@ArchTest
	static final ArchRule applicationMustNotDependOnApi = noClasses()
			.that().resideInAPackage("com.altafjava.school.application..")
			.should().dependOnClassesThat().resideInAPackage("com.altafjava.school.api..")
			.because(
					"application orchestrates domain use cases — it must never import the api layer built on top of it");
}
