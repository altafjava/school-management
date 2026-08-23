package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.teacher.model.EmploymentType;

public record UpdateTeacherHrDetailsRequest(
		String departmentPublicId,
		@Size(max = 255) String qualification,
		EmploymentType employmentType) {
}
