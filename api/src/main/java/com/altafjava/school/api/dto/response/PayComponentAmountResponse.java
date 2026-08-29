package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import com.altafjava.school.domain.payroll.model.PayComponentType;

public record PayComponentAmountResponse(
		String code,
		String name,
		PayComponentType type,
		BigDecimal amount) {
}
