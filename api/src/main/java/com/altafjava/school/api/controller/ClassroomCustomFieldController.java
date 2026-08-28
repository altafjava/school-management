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
import com.altafjava.school.api.mapper.CustomFieldValueMapper;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.CustomFieldValueService;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;

// A clearly-scoped sibling to ClassroomController rather than an extension of it — see
// StudentCustomFieldController's own Javadoc for the same rationale.
@RestController
@RequestMapping("/api/v1/classrooms/{publicId}/custom-fields")
public class ClassroomCustomFieldController {

	private final ClassroomService classroomService;
	private final CustomFieldValueService customFieldValueService;
	private final CustomFieldValueMapper customFieldValueMapper;

	public ClassroomCustomFieldController(ClassroomService classroomService,
			CustomFieldValueService customFieldValueService,
			CustomFieldValueMapper customFieldValueMapper) {
		this.classroomService = classroomService;
		this.customFieldValueService = customFieldValueService;
		this.customFieldValueMapper = customFieldValueMapper;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public List<CustomFieldValueResponse> get(@PathVariable String publicId) {
		Classroom classroom = classroomService.findByPublicId(publicId);
		return customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.CLASSROOM, classroom.getId()));
	}

	@PutMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_WRITE')")
	public List<CustomFieldValueResponse> set(@PathVariable String publicId,
			@Valid @RequestBody SetCustomFieldValuesRequest request) {
		Classroom classroom = classroomService.findByPublicId(publicId);
		for (Map.Entry<String, String> entry : request.values().entrySet()) {
			customFieldValueService.setValue(CustomFieldEntityType.CLASSROOM, classroom.getId(), entry.getKey(),
					entry.getValue());
		}
		return customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.CLASSROOM, classroom.getId()));
	}
}
