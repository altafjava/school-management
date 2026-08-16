package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.grade.model.GradeThreshold;
import com.altafjava.school.domain.grade.model.GradingScale;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GradingScaleServiceTest {

	@Mock
	private TenantSettingOverrideService tenantSettingOverrideService;

	private GradingScaleService gradingScaleService;

	@BeforeEach
	void setUp() {
		gradingScaleService = new GradingScaleService(tenantSettingOverrideService, new ObjectMapper());
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void getScale_noOverrideConfigured_returnsDefaultScale() {
		when(tenantSettingOverrideService.get(1L, GradingScaleService.SETTING_KEY)).thenReturn(Optional.empty());

		GradingScale scale = gradingScaleService.getScale();

		assertEquals(GradingScale.defaultScale().thresholds(), scale.thresholds());
	}

	@Test
	void updateScale_thenGetScale_roundTripsThroughJson() {
		List<GradeThreshold> thresholds = List.of(
				new GradeThreshold("PASS", BigDecimal.valueOf(50)),
				new GradeThreshold("FAIL", BigDecimal.ZERO));

		gradingScaleService.updateScale(thresholds);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(tenantSettingOverrideService).set(eq(1L), eq(GradingScaleService.SETTING_KEY), captor.capture());

		when(tenantSettingOverrideService.get(1L, GradingScaleService.SETTING_KEY))
				.thenReturn(Optional.of(captor.getValue()));

		GradingScale roundTripped = gradingScaleService.getScale();

		assertEquals("PASS", roundTripped.thresholds().get(0).letter());
	}

	@Test
	void getScale_corruptJson_throwsBusinessException() {
		when(tenantSettingOverrideService.get(1L, GradingScaleService.SETTING_KEY))
				.thenReturn(Optional.of("not-valid-json"));

		assertThrows(BusinessException.class, () -> gradingScaleService.getScale());
	}
}
