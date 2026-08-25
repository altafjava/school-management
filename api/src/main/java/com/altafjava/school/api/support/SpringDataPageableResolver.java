package com.altafjava.school.api.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import com.altafjava.platform.api.support.PageableParamResolver;
import lombok.RequiredArgsConstructor;

/**
 * Adapts platform's {@link PageableParamResolver} — which resolves {@code page}/{@code size} into
 * the platform-generic {@code core.model.Pageable} record, bounds-checked against
 * {@code PlatformConfigurer#maxPageSize()}/{@code defaultPageSize()} — into Spring Data's
 * {@link Pageable}, since every school-saas repository still queries via Spring Data JPA.
 * Controllers call this instead of hand-rolling {@code PageRequest.of(page, Math.min(size, 100))}.
 */
@Component
@RequiredArgsConstructor
public class SpringDataPageableResolver {

	private final PageableParamResolver pageableParamResolver;

	public Pageable resolve(int page, int size) {
		var resolved = pageableParamResolver.resolve(page, size);
		return PageRequest.of(resolved.page(), resolved.size());
	}
}
