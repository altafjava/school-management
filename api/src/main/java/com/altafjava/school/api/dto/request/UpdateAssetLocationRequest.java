package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateAssetLocationRequest(@Size(max = 150) String location) {
}
