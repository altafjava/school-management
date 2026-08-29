package com.altafjava.school.api.support;

/**
 * Converts Spring Data's {@link org.springframework.data.domain.Page} to platform's own
 * {@link com.altafjava.platform.core.model.Page} at the API boundary.
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
