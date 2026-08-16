package com.altafjava.school.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Architecture fitness test guarding against a JPA {@code @Entity} being returned directly from a
 * REST endpoint, violating {@code CLAUDE.md}'s hard rule that controllers return DTOs, never JPA
 * entities. Mirrors {@code platform-saas}'s own {@code EntityLeakageFitnessTest}, written after
 * that exact defect was found across ~15 platform endpoints. Inspects each HTTP-mapped method's
 * generic return type (unwrapping {@code Page}/{@code List}/etc.) so a future controller can't
 * reintroduce this even through a container type not explicitly anticipated here.
 */
@AnalyzeClasses(packages = "com.altafjava.school.api.controller")
class EntityLeakageFitnessTest {

	@ArchTest
	static final ArchRule controllersMustNotReturnJpaEntities = classes()
			.that().areAnnotatedWith(RestController.class)
			.should(notExposeJpaEntitiesFromHttpEndpoints())
			.because("REST controllers must return DTOs — a JPA @Entity returned directly (even nested inside "
					+ "Page/List) leaks persistence-only fields and couples the API contract to the schema");

	private static ArchCondition<JavaClass> notExposeJpaEntitiesFromHttpEndpoints() {
		return new ArchCondition<>(
				"not return a JPA @Entity type from an HTTP-mapped method, directly or wrapped in a generic container") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				for (JavaMethod method : javaClass.getMethods()) {
					if (!isHttpEndpoint(method)) {
						continue;
					}
					Class<?> leakedEntity = findLeakedEntity(method.reflect().getGenericReturnType());
					if (leakedEntity != null) {
						events.add(SimpleConditionEvent.violated(method,
								method.getFullName() + "() returns " + leakedEntity.getName()
										+ ", a JPA @Entity — return a dedicated response DTO (record) instead"));
					}
				}
			}
		};
	}

	private static boolean isHttpEndpoint(JavaMethod method) {
		return method.isAnnotatedWith(GetMapping.class)
				|| method.isAnnotatedWith(PostMapping.class)
				|| method.isAnnotatedWith(PutMapping.class)
				|| method.isAnnotatedWith(PatchMapping.class)
				|| method.isAnnotatedWith(DeleteMapping.class)
				|| method.isAnnotatedWith(RequestMapping.class);
	}

	private static Class<?> findLeakedEntity(Type type) {
		if (type instanceof Class<?> clazz) {
			return clazz.isAnnotationPresent(Entity.class) ? clazz : null;
		}
		if (type instanceof ParameterizedType parameterizedType) {
			for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
				Class<?> found = findLeakedEntity(typeArgument);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
