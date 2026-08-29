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
import com.altafjava.school.api.controller.api.SubjectCustomFieldApi;
import com.altafjava.school.api.dto.request.SetCustomFieldValuesRequest;
import com.altafjava.school.api.dto.response.CustomFieldValueResponse;
import com.altafjava.school.api.mapper.CustomFieldValueMapper;
import com.altafjava.school.application.service.CustomFieldValueService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.subject.model.Subject;

// A clearly-scoped sibling to SubjectController rather than an extension of it — see
// StudentCustomFieldController's own Javadoc for the same rationale.
@RestController
@RequestMapping("/api/v1/subjects/{publicId}/custom-fields")
public class SubjectCustomFieldController implements SubjectCustomFieldApi {

	private final SubjectService subjectService;
	private final CustomFieldValueService customFieldValueService;
	private final CustomFieldValueMapper customFieldValueMapper;

	public SubjectCustomFieldController(SubjectService subjectService, CustomFieldValueService customFieldValueService,
			CustomFieldValueMapper customFieldValueMapper) {
		this.subjectService = subjectService;
		this.customFieldValueService = customFieldValueService;
		this.customFieldValueMapper = customFieldValueMapper;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public ApiResponse<List<CustomFieldValueResponse>> get(@PathVariable String publicId) {
		Subject subject = subjectService.findByPublicId(publicId);
		return ApiResponse.success(customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.SUBJECT, subject.getId())));
	}

	@Override
	@PutMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_WRITE')")
	public ApiResponse<List<CustomFieldValueResponse>> set(@PathVariable String publicId,
			@Valid @RequestBody SetCustomFieldValuesRequest request) {
		Subject subject = subjectService.findByPublicId(publicId);
		for (Map.Entry<String, String> entry : request.values().entrySet()) {
			customFieldValueService.setValue(CustomFieldEntityType.SUBJECT, subject.getId(), entry.getKey(),
					entry.getValue());
		}
		return ApiResponse.success(customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.SUBJECT, subject.getId())));
	}
}
