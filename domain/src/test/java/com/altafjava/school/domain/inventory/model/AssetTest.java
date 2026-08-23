package com.altafjava.school.domain.inventory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class AssetTest {

	private Asset created() {
		return Asset.create("AST-1", "Projector", "Electronics", LocalDate.of(2026, 1, 1),
				BigDecimal.valueOf(500), "Room 101");
	}

	@Test
	void create_startsAvailable() {
		Asset asset = created();

		assertEquals(AssetStatus.AVAILABLE, asset.getStatus());
	}

	@Test
	void markInUse_fromAvailable_succeeds() {
		Asset asset = created();

		asset.markInUse();

		assertEquals(AssetStatus.IN_USE, asset.getStatus());
	}

	@Test
	void markInUse_whenAlreadyInUse_throwsBusinessException() {
		Asset asset = created();
		asset.markInUse();

		assertThrows(BusinessException.class, asset::markInUse);
	}

	@Test
	void markAvailable_afterInUse_succeeds() {
		Asset asset = created();
		asset.markInUse();

		asset.markAvailable();

		assertEquals(AssetStatus.AVAILABLE, asset.getStatus());
	}

	@Test
	void markAvailable_whenDisposed_throwsBusinessException() {
		Asset asset = created();
		asset.markDisposed();

		assertThrows(BusinessException.class, asset::markAvailable);
	}

	@Test
	void markUnderMaintenance_whenDisposed_throwsBusinessException() {
		Asset asset = created();
		asset.markDisposed();

		assertThrows(BusinessException.class, asset::markUnderMaintenance);
	}

	@Test
	void markDisposed_fromAnyStatus_succeeds() {
		Asset asset = created();
		asset.markInUse();

		asset.markDisposed();

		assertEquals(AssetStatus.DISPOSED, asset.getStatus());
	}
}
