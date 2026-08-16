package com.altafjava.school.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Every request/response DTO must be a Java record (CLAUDE.md § DTOs: "Java records only") —
 * immutable, no accidental mutation after validation, no Lombok-generated equals/hashCode drift.
 */
@AnalyzeClasses(packages = "com.altafjava.school.api.dto")
class DtoRecordFitnessTest {

	@ArchTest
	static final ArchRule dtosMustBeRecords = classes()
			.that()
			.resideInAnyPackage("com.altafjava.school.api.dto.request..", "com.altafjava.school.api.dto.response..")
			.should(beARecord())
			.because("CLAUDE.md mandates DTOs are Java records only — immutable, no accidental mutation "
					+ "after validation");

	private static ArchCondition<JavaClass> beARecord() {
		return new ArchCondition<>("be a Java record") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				if (!javaClass.reflect().isRecord()) {
					events.add(SimpleConditionEvent.violated(javaClass,
							javaClass.getFullName() + " is a DTO but not a Java record"));
				}
			}
		};
	}
}
