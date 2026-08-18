package com.altafjava.school.api.dto.request;

// Exactly one of studentPublicId / classroomPublicId must be set — enforced by FeeAssignmentService.
public record AssignFeeStructureRequest(String studentPublicId, String classroomPublicId) {
}
