package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;

public record LessonResponse(
		String publicId,
		Long classroomId,
		Long subjectId,
		Long teacherId,
		String title,
		String description,
		String storageKey,
		LocalDateTime postedAt) {
}
