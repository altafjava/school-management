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
import com.altafjava.school.api.dto.request.SetCustomFieldValuesRequest;
import com.altafjava.school.api.dto.response.CustomFieldValueResponse;
import com.altafjava.school.api.dto.response.FieldGroupResponse;
import com.altafjava.school.api.mapper.CustomFieldValueMapper;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.application.service.CustomFieldValueService;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;

// A clearly-scoped sibling to AdmissionController, mirroring StudentCustomFieldController's shape.
@RestController
@RequestMapping("/api/v1/admissions/{publicId}/custom-fields")
public class AdmissionCustomFieldController {

	private final AdmissionService admissionService;
	private final CustomFieldValueService customFieldValueService;
	private final CustomFieldValueMapper customFieldValueMapper;

	public AdmissionCustomFieldController(AdmissionService admissionService,
			CustomFieldValueService customFieldValueService, CustomFieldValueMapper customFieldValueMapper) {
		this.admissionService = admissionService;
		this.customFieldValueService = customFieldValueService;
		this.customFieldValueMapper = customFieldValueMapper;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public List<CustomFieldValueResponse> get(@PathVariable String publicId) {
		Admission admission = admissionService.findByPublicId(publicId);
		return customFieldValueMapper
				.toResponseList(
						customFieldValueService.getAllValues(CustomFieldEntityType.ADMISSION, admission.getId()));
	}

	@GetMapping("/grouped")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public List<FieldGroupResponse> getGrouped(@PathVariable String publicId) {
		Admission admission = admissionService.findByPublicId(publicId);
		return customFieldValueMapper.toGroupResponseList(
				customFieldValueService.getGroupedValues(CustomFieldEntityType.ADMISSION, admission.getId()));
	}

	@PutMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_WRITE')")
	public List<CustomFieldValueResponse> set(@PathVariable String publicId,
			@Valid @RequestBody SetCustomFieldValuesRequest request) {
		Admission admission = admissionService.findByPublicId(publicId);
		for (Map.Entry<String, String> entry : request.values().entrySet()) {
			customFieldValueService.setValue(CustomFieldEntityType.ADMISSION, admission.getId(), entry.getKey(),
					entry.getValue());
		}
		return customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.ADMISSION, admission.getId()));
	}
}
