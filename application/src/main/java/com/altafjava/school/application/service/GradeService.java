package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.application.security.TeacherClassroomScopeResolver;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.model.GradeCorrection;
import com.altafjava.school.domain.grade.repository.GradeCorrectionRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.grade.service.GradeCalculator;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class GradeService {

	private final GradeRepository gradeRepository;
	private final GradeCorrectionRepository gradeCorrectionRepository;
	private final StudentRepository studentRepository;
	private final ExamRepository examRepository;
	private final GradingScaleService gradingScaleService;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final TeacherClassroomScopeResolver teacherClassroomScopeResolver;
	private final GradeCalculator gradeCalculator = new GradeCalculator();

	public GradeService(GradeRepository gradeRepository, GradeCorrectionRepository gradeCorrectionRepository,
			StudentRepository studentRepository, ExamRepository examRepository,
			GradingScaleService gradingScaleService, StudentDataAccessGuard studentDataAccessGuard,
			TeacherClassroomScopeResolver teacherClassroomScopeResolver) {
		this.gradeRepository = gradeRepository;
		this.gradeCorrectionRepository = gradeCorrectionRepository;
		this.studentRepository = studentRepository;
		this.examRepository = examRepository;
		this.gradingScaleService = gradingScaleService;
		this.studentDataAccessGuard = studentDataAccessGuard;
		this.teacherClassroomScopeResolver = teacherClassroomScopeResolver;
	}

	// TENANT_ADMIN sees every grade; TEACHER sees only grades from exams in classrooms they
	// teach (resolved via TeacherClassroomScopeResolver — see ROADMAP.md Phase 3).
	@Transactional(readOnly = true)
	public Page<Grade> listGrades(Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return teacherClassroomScopeResolver.resolveClassroomIdsIfTeacherScoped(tenantId)
				.map(classroomIds -> {
					List<Long> examIds = examRepository.findIdsByClassroomIdInAndTenantId(classroomIds, tenantId);
					return gradeRepository.findByExamIdInAndTenantId(examIds, tenantId, pageable);
				})
				.orElseGet(() -> gradeRepository.findAllByTenantId(tenantId, pageable));
	}

	@Transactional(readOnly = true)
	public Grade findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return gradeRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Grade not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public Page<Grade> getStudentGrades(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return gradeRepository.findByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional
	public Grade record(Long studentId, Long examId, BigDecimal marks, String gradedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!studentRepository.existsByIdAndTenantId(studentId, tenantId)) {
			throw new ResourceNotFoundException("Student not found: " + studentId);
		}
		Exam exam = examRepository.findByIdAndTenantId(examId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
		if (gradeRepository.existsByStudentIdAndExamIdAndTenantId(studentId, examId, tenantId)) {
			throw new IllegalArgumentException(
					"Grade already recorded for student " + studentId + " in exam " + examId);
		}
		List<GradingScaleThreshold> thresholds = gradingScaleService.resolveEffectiveThresholds(exam.getClassroomId());
		String gradeLetter = gradeCalculator.calculateLetterGrade(marks, exam.getMaxMarks(), thresholds);
		// subjectId is derived from the exam, not client-supplied — a grade's subject must always
		// match its exam's subject, so there is no legitimate case where they could differ.
		Grade grade = Grade.create(studentId, exam.getSubjectId(), examId, marks, gradeLetter, gradedBy);
		return gradeRepository.save(grade);
	}

	/**
	 * Corrects an already-recorded grade's marks, recalculating the letter grade against the
	 * classroom's currently effective grading scale (same resolution {@link #record} uses).
	 * Records a {@link GradeCorrection} row with the pre-correction values before mutating, so the
	 * correction is reconstructable from history rather than only visible as an opaque
	 * {@code updatedAt} bump.
	 */
	@Transactional
	public Grade correct(String publicId, BigDecimal marks) {
		Grade grade = findByPublicId(publicId);
		Long tenantId = TenantContext.getCurrentTenantId();
		Exam exam = examRepository.findByIdAndTenantId(grade.getExamId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + grade.getExamId()));
		List<GradingScaleThreshold> thresholds = gradingScaleService.resolveEffectiveThresholds(exam.getClassroomId());
		String newGradeLetter = gradeCalculator.calculateLetterGrade(marks, exam.getMaxMarks(), thresholds);

		gradeCorrectionRepository.save(GradeCorrection.record(grade.getId(), grade.getMarks(), grade.getGradeLetter(),
				marks, newGradeLetter));

		grade.correct(marks, newGradeLetter);
		return gradeRepository.save(grade);
	}

	@Transactional(readOnly = true)
	public Page<GradeCorrection> listCorrections(String gradePublicId, Pageable pageable) {
		Grade grade = findByPublicId(gradePublicId);
		return gradeCorrectionRepository.findByGradeIdAndTenantId(TenantContext.getCurrentTenantId(), grade.getId(),
				pageable);
	}
}
