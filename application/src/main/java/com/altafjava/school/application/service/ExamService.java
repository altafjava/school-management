package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@Service
public class ExamService {

	private final ExamRepository examRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;

	public ExamService(ExamRepository examRepository, ClassroomRepository classroomRepository,
			SubjectRepository subjectRepository) {
		this.examRepository = examRepository;
		this.classroomRepository = classroomRepository;
		this.subjectRepository = subjectRepository;
	}

	@Transactional(readOnly = true)
	public Page<Exam> listExams(Pageable pageable) {
		return examRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Exam findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return examRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + publicId));
	}

	@Transactional
	public Exam schedule(String title, Long subjectId, Long classroomId,
			LocalDateTime scheduledAt, BigDecimal maxMarks) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!classroomRepository.existsByIdAndTenantId(classroomId, tenantId)) {
			throw new ResourceNotFoundException("Classroom not found: " + classroomId);
		}
		if (!subjectRepository.existsByIdAndTenantId(subjectId, tenantId)) {
			throw new ResourceNotFoundException("Subject not found: " + subjectId);
		}
		Exam exam = Exam.create(title, subjectId, classroomId, scheduledAt, maxMarks);
		return examRepository.save(exam);
	}
}
