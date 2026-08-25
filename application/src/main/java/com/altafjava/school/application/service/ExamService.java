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
import com.altafjava.school.domain.exam.model.ExamType;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.repository.TermRepository;

@Service
public class ExamService {

	private final ExamRepository examRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final TermRepository termRepository;

	public ExamService(ExamRepository examRepository, ClassroomRepository classroomRepository,
			SubjectRepository subjectRepository, TermRepository termRepository) {
		this.examRepository = examRepository;
		this.classroomRepository = classroomRepository;
		this.subjectRepository = subjectRepository;
		this.termRepository = termRepository;
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
			LocalDateTime scheduledAt, BigDecimal maxMarks, Long termId, ExamType examType) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!classroomRepository.existsByIdAndTenantId(classroomId, tenantId)) {
			throw new ResourceNotFoundException("Classroom not found: " + classroomId);
		}
		if (!subjectRepository.existsByIdAndTenantId(subjectId, tenantId)) {
			throw new ResourceNotFoundException("Subject not found: " + subjectId);
		}
		if (termId != null && !termRepository.existsByIdAndTenantId(termId, tenantId)) {
			throw new ResourceNotFoundException("Term not found: " + termId);
		}
		Exam exam = Exam.create(title, subjectId, classroomId, scheduledAt, maxMarks, termId, examType);
		return examRepository.save(exam);
	}

	@Transactional
	public Exam reschedule(String publicId, LocalDateTime scheduledAt) {
		Exam exam = findByPublicId(publicId);
		exam.reschedule(scheduledAt);
		return examRepository.save(exam);
	}

	@Transactional
	public Exam assignTerm(String publicId, Long termId) {
		Exam exam = findByPublicId(publicId);
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!termRepository.existsByIdAndTenantId(termId, tenantId)) {
			throw new ResourceNotFoundException("Term not found: " + termId);
		}
		exam.assignTerm(termId);
		return examRepository.save(exam);
	}

	@Transactional
	public Exam complete(String publicId) {
		Exam exam = findByPublicId(publicId);
		exam.complete();
		return examRepository.save(exam);
	}

	@Transactional
	public Exam cancel(String publicId) {
		Exam exam = findByPublicId(publicId);
		exam.cancel();
		return examRepository.save(exam);
	}
}
