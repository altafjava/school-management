package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.altafjava.platform.application.branding.TenantBrandingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.file.service.StorageService;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.application.certificate.CertificatePdfGenerator;
import com.altafjava.school.application.certificate.CertificateVerificationResult;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.certificate.model.CertificateIssuance;
import com.altafjava.school.domain.certificate.model.CertificateTemplate;
import com.altafjava.school.domain.certificate.repository.CertificateIssuanceRepository;
import com.altafjava.school.domain.certificate.repository.CertificateTemplateRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

	@Mock
	private CertificateIssuanceRepository certificateIssuanceRepository;
	@Mock
	private CertificateTemplateRepository certificateTemplateRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private StorageService storageService;
	@Mock
	private CertificatePdfGenerator pdfGenerator;
	@Mock
	private PlatformTransactionManager transactionManager;
	@Mock
	private TenantRepository tenantRepository;
	@Mock
	private TenantBrandingService tenantBrandingService;

	private CertificateService certificateService;

	@BeforeEach
	void setUp() {
		lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
		Tenant tenant = Tenant.builder().name("Test School").build();
		lenient().when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
		lenient().when(tenantBrandingService.getLogoBytes(1L)).thenReturn(Optional.empty());
		certificateService = new CertificateService(certificateIssuanceRepository, certificateTemplateRepository,
				studentRepository, studentClassroomLinkRepository, classroomRepository, academicYearRepository,
				storageService, pdfGenerator, transactionManager, tenantRepository, tenantBrandingService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student studentWithPublicId(long id, UUID publicId) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(id);
		student.setPublicId(publicId);
		return student;
	}

	private CertificateTemplate activeTemplateWithPublicId(long id, UUID publicId) {
		CertificateTemplate template = CertificateTemplate.create("Bonafide Certificate",
				"This certifies {{studentName}} of {{className}}, admitted {{admissionDate}}.");
		template.setId(id);
		template.setPublicId(publicId);
		return template;
	}

	@Test
	void issue_happyPath_resolvesPlaceholdersUploadsAndPersistsIssuance() {
		UUID studentPublicId = UUID.randomUUID();
		UUID templatePublicId = UUID.randomUUID();
		Student student = studentWithPublicId(1L, studentPublicId);
		CertificateTemplate template = activeTemplateWithPublicId(10L, templatePublicId);
		StudentClassroomLink link = StudentClassroomLink.create(1L, 5L, 7L, LocalDate.of(2020, 6, 1));
		Classroom classroom = Classroom.create("5A", "Grade 5", "A", 7L, "2025-26", null);
		AcademicYear year = AcademicYear.create("2025-26", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 4, 30), true);
		year.setId(7L);

		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(certificateTemplateRepository.findByPublicIdAndTenantId(templatePublicId, 1L))
				.thenReturn(Optional.of(template));
		when(studentClassroomLinkRepository.findByStudentId(1L, 1L)).thenReturn(List.of(link));
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.of(year));
		when(classroomRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(classroom));
		when(academicYearRepository.findByIdAndTenantId(7L, 1L)).thenReturn(Optional.of(year));
		when(pdfGenerator.generate(eq("Bonafide Certificate"), anyString(), anyString(), any()))
				.thenReturn("pdf-bytes".getBytes());
		when(certificateIssuanceRepository.existsByVerificationCodeAndTenantId(anyString(), eq(1L)))
				.thenReturn(false);
		when(certificateIssuanceRepository.save(any(CertificateIssuance.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CertificateIssuance result = certificateService.issue(studentPublicId.toString(), templatePublicId.toString(),
				99L);

		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(pdfGenerator).generate(eq("Bonafide Certificate"), bodyCaptor.capture(), anyString(), any());
		assertEquals("This certifies Alice Smith of Grade 5 A, admitted 2020-06-01.", bodyCaptor.getValue());
		assertEquals(1L, result.getStudentId());
		assertEquals(10L, result.getCertificateTemplateId());
		assertEquals(99L, result.getIssuedByUserId());
		assertNotNull(result.getVerificationCode());
		verify(storageService).uploadFile(anyString(), any(byte[].class), eq("application/pdf"));
	}

	@Test
	void issue_withInactiveTemplate_throwsBusinessException() {
		UUID studentPublicId = UUID.randomUUID();
		UUID templatePublicId = UUID.randomUUID();
		Student student = studentWithPublicId(1L, studentPublicId);
		CertificateTemplate template = activeTemplateWithPublicId(10L, templatePublicId);
		template.deactivate();

		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(certificateTemplateRepository.findByPublicIdAndTenantId(templatePublicId, 1L))
				.thenReturn(Optional.of(template));

		assertThrows(BusinessException.class,
				() -> certificateService.issue(studentPublicId.toString(), templatePublicId.toString(), 99L));
		verify(storageService, never()).uploadFile(anyString(), any(byte[].class), anyString());
	}

	@Test
	void issue_withNonExistentStudent_throwsResourceNotFound() {
		UUID studentPublicId = UUID.randomUUID();
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> certificateService.issue(studentPublicId.toString(), UUID.randomUUID().toString(), 99L));
	}

	@Test
	void issue_whenDbWriteFailsAfterSuccessfulUpload_cleansUpOrphanedUploadAndRethrows() {
		UUID studentPublicId = UUID.randomUUID();
		UUID templatePublicId = UUID.randomUUID();
		Student student = studentWithPublicId(1L, studentPublicId);
		CertificateTemplate template = activeTemplateWithPublicId(10L, templatePublicId);

		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(certificateTemplateRepository.findByPublicIdAndTenantId(templatePublicId, 1L))
				.thenReturn(Optional.of(template));
		when(studentClassroomLinkRepository.findByStudentId(1L, 1L)).thenReturn(List.of());
		when(pdfGenerator.generate(anyString(), anyString(), anyString(), any())).thenReturn("pdf-bytes".getBytes());
		when(certificateIssuanceRepository.existsByVerificationCodeAndTenantId(anyString(), eq(1L)))
				.thenReturn(false);
		when(certificateIssuanceRepository.save(any(CertificateIssuance.class)))
				.thenThrow(new RuntimeException("db unavailable"));

		assertThrows(RuntimeException.class,
				() -> certificateService.issue(studentPublicId.toString(), templatePublicId.toString(), 99L));

		ArgumentCaptor<String> uploadedKeyCaptor = ArgumentCaptor.forClass(String.class);
		verify(storageService).uploadFile(uploadedKeyCaptor.capture(), any(byte[].class), eq("application/pdf"));
		verify(storageService).deleteFile(uploadedKeyCaptor.getValue());
	}

	@Test
	void verify_withValidCode_returnsMinimalConfirmation() {
		Student student = studentWithPublicId(1L, UUID.randomUUID());
		CertificateTemplate template = activeTemplateWithPublicId(10L, UUID.randomUUID());
		CertificateIssuance issuance = CertificateIssuance.create(1L, 10L, "code123", "key.pdf", 99L);

		when(certificateIssuanceRepository.findByVerificationCodeAndTenantId("code123", 1L))
				.thenReturn(Optional.of(issuance));
		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(certificateTemplateRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(template));

		CertificateVerificationResult result = certificateService.verify("code123");

		assertEquals("Alice Smith", result.studentName());
		assertEquals("Bonafide Certificate", result.certificateName());
		assertNotNull(result.issuedAt());
	}

	@Test
	void verify_withUnknownCode_throwsResourceNotFound() {
		when(certificateIssuanceRepository.findByVerificationCodeAndTenantId("unknown", 1L))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> certificateService.verify("unknown"));
	}
}
