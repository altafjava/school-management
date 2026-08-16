package com.altafjava.school.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.grade.model.GradeThreshold;
import com.altafjava.school.domain.grade.model.GradingScale;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Bridges GradingScale to TenantSettingOverride; tenants without one get GradingScale.defaultScale().
@Service
@RequiredArgsConstructor
public class GradingScaleService {

	static final String SETTING_KEY = "school.grading.scale";

	private final TenantSettingOverrideService tenantSettingOverrideService;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public GradingScale getScale() {
		Long tenantId = TenantContext.getCurrentTenantId();
		return tenantSettingOverrideService.get(tenantId, SETTING_KEY)
				.map(this::parse)
				.orElseGet(GradingScale::defaultScale);
	}

	@Transactional
	public GradingScale updateScale(List<GradeThreshold> thresholds) {
		GradingScale scale = GradingScale.of(thresholds);
		Long tenantId = TenantContext.getCurrentTenantId();
		tenantSettingOverrideService.set(tenantId, SETTING_KEY, serialize(scale));
		return scale;
	}

	private GradingScale parse(String json) {
		try {
			List<GradeThreshold> thresholds = objectMapper.readValue(json, new TypeReference<List<GradeThreshold>>() {
			});
			return GradingScale.of(thresholds);
		} catch (JacksonException | IllegalArgumentException e) {
			throw new BusinessException("Corrupt grading scale setting for tenant " + TenantContext
					.getCurrentTenantId());
		}
	}

	private String serialize(GradingScale scale) {
		try {
			return objectMapper.writeValueAsString(scale.thresholds());
		} catch (JacksonException e) {
			throw new BusinessException("Failed to serialize grading scale");
		}
	}
}
