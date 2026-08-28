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
import com.altafjava.school.application.service.CustomFieldValueService;
import com.altafjava.school.application.service.LeaveTypeService;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.leave.model.LeaveType;

// A clearly-scoped sibling to LeaveTypeController rather than an extension of it — see
// StudentCustomFieldController's own Javadoc for the same rationale.
@RestController
@RequestMapping("/api/v1/leave-types/{publicId}/custom-fields")
public class LeaveTypeCustomFieldController {

	private final LeaveTypeService leaveTypeService;
	private final CustomFieldValueService customFieldValueService;
	private final CustomFieldValueMapper customFieldValueMapper;

	public LeaveTypeCustomFieldController(LeaveTypeService leaveTypeService,
			CustomFieldValueService customFieldValueService,
			CustomFieldValueMapper customFieldValueMapper) {
		this.leaveTypeService = leaveTypeService;
		this.customFieldValueService = customFieldValueService;
		this.customFieldValueMapper = customFieldValueMapper;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public List<CustomFieldValueResponse> get(@PathVariable String publicId) {
		LeaveType leaveType = leaveTypeService.findByPublicId(publicId);
		return customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.LEAVE_TYPE, leaveType.getId()));
	}

	@PutMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_WRITE')")
	public List<CustomFieldValueResponse> set(@PathVariable String publicId,
			@Valid @RequestBody SetCustomFieldValuesRequest request) {
		LeaveType leaveType = leaveTypeService.findByPublicId(publicId);
		for (Map.Entry<String, String> entry : request.values().entrySet()) {
			customFieldValueService.setValue(CustomFieldEntityType.LEAVE_TYPE, leaveType.getId(), entry.getKey(),
					entry.getValue());
		}
		return customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.LEAVE_TYPE, leaveType.getId()));
	}
}
