package com.altafjava.school.api.support;

/**
 * Converts a Spring Data {@link org.springframework.data.domain.Page} (what every school-saas
 * repository query returns) into platform's own {@link com.altafjava.platform.core.model.Page}
 * (what every {@code ApiResponse}-wrapped list endpoint returns) — the one adapter point at the
 * API boundary, mirroring {@link SpringDataPageableResolver}'s role on the request side. A pure,
 * stateless conversion function — no reason for it to be a Spring bean.
 */
public final class PlatformPageMapper {

	private PlatformPageMapper() {
	}

	public static <T> com.altafjava.platform.core.model.Page<T> toPlatformPage(
			org.springframework.data.domain.Page<T> springPage) {
		return new com.altafjava.platform.core.model.Page<>(
				springPage.getContent(),
				springPage.getNumber(),
				springPage.getSize(),
				springPage.getTotalElements());
	}
}
