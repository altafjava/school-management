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
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.fee.model.FeeStructure;

// A clearly-scoped sibling to FeeStructureController rather than an extension of it — see
// StudentCustomFieldController's own Javadoc for the same rationale.
@RestController
@RequestMapping("/api/v1/fee-structures/{publicId}/custom-fields")
public class FeeStructureCustomFieldController {

	private final FeeStructureService feeStructureService;
	private final CustomFieldValueService customFieldValueService;
	private final CustomFieldValueMapper customFieldValueMapper;

	public FeeStructureCustomFieldController(FeeStructureService feeStructureService,
			CustomFieldValueService customFieldValueService,
			CustomFieldValueMapper customFieldValueMapper) {
		this.feeStructureService = feeStructureService;
		this.customFieldValueService = customFieldValueService;
		this.customFieldValueMapper = customFieldValueMapper;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public List<CustomFieldValueResponse> get(@PathVariable String publicId) {
		FeeStructure feeStructure = feeStructureService.findByPublicId(publicId);
		return customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.FEE_STRUCTURE, feeStructure.getId()));
	}

	@PutMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_WRITE')")
	public List<CustomFieldValueResponse> set(@PathVariable String publicId,
			@Valid @RequestBody SetCustomFieldValuesRequest request) {
		FeeStructure feeStructure = feeStructureService.findByPublicId(publicId);
		for (Map.Entry<String, String> entry : request.values().entrySet()) {
			customFieldValueService.setValue(CustomFieldEntityType.FEE_STRUCTURE, feeStructure.getId(), entry.getKey(),
					entry.getValue());
		}
		return customFieldValueMapper.toResponseList(
				customFieldValueService.getAllValues(CustomFieldEntityType.FEE_STRUCTURE, feeStructure.getId()));
	}
}
