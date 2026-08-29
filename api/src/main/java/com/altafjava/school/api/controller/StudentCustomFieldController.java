package com.altafjava.school.api.controller;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.StudentCustomFieldApi;
import com.altafjava.school.api.dto.request.SetCustomFieldValuesRequest;
import com.altafjava.school.api.dto.response.CustomFieldValueResponse;
import com.altafjava.school.api.dto.response.FieldGroupResponse;
import com.altafjava.school.api.mapper.CustomFieldValueMapper;
import com.altafjava.school.application.service.CustomFieldValueService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.student.model.Student;

// A clearly-scoped sibling to StudentController rather than an extension of it — StudentController
// already carries a wide surface (grades/attendance/fees/report-cards); custom fields are a
// self-contained concern with their own request/response shapes and no other student endpoint
// needs.
@RestController
@RequestMapping("/api/v1/students/{publicId}/custom-fields")
public class StudentCustomFieldController implements StudentCustomFieldApi {

	private final StudentService studentService;
	private final CustomFieldValueService customFieldValueService;
	private final CustomFieldValueMapper customFieldValueMapper;

	public StudentCustomFieldController(StudentService studentService,
			CustomFieldValueService customFieldValueService, CustomFieldValueMapper customFieldValueMapper) {
		this.studentService = studentService;
		this.customFieldValueService = customFieldValueService;
		this.customFieldValueMapper = customFieldValueMapper;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public ApiResponse<List<CustomFieldValueResponse>> get(@PathVariable String publicId) {
		Student student = studentService.findByPublicId(publicId);
		return ApiResponse.success(customFieldValueMapper
				.toResponseList(customFieldValueService.getAllValues(CustomFieldEntityType.STUDENT, student.getId())));
	}

	// Same values as get(), grouped by displayGroup (ordered by displayGroupOrder) and each tagged
	// with a resolved visible flag from its visibilityCondition — a caller renders exactly this,
	// no group/order/conditional-visibility logic needed client-side.
	@Override
	@GetMapping("/grouped")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public ApiResponse<List<FieldGroupResponse>> getGrouped(@PathVariable String publicId) {
		Student student = studentService.findByPublicId(publicId);
		return ApiResponse.success(customFieldValueMapper.toGroupResponseList(
				customFieldValueService.getGroupedValues(CustomFieldEntityType.STUDENT, student.getId())));
	}

	@Override
	@PutMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_WRITE')")
	public ApiResponse<List<CustomFieldValueResponse>> set(@PathVariable String publicId,
			@Valid @RequestBody SetCustomFieldValuesRequest request) {
		Student student = studentService.findByPublicId(publicId);
		for (Map.Entry<String, String> entry : request.values().entrySet()) {
			customFieldValueService.setValue(CustomFieldEntityType.STUDENT, student.getId(), entry.getKey(),
					entry.getValue());
		}
		return ApiResponse.success(customFieldValueMapper
				.toResponseList(customFieldValueService.getAllValues(CustomFieldEntityType.STUDENT, student.getId())));
	}
}
