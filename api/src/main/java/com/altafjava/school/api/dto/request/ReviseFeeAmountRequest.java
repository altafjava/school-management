package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ReviseFeeAmountRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {
}
