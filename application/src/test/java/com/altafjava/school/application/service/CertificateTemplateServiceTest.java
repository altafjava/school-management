package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.certificate.model.CertificateTemplate;
import com.altafjava.school.domain.certificate.repository.CertificateTemplateRepository;

@ExtendWith(MockitoExtension.class)
class CertificateTemplateServiceTest {

	@Mock
	private CertificateTemplateRepository certificateTemplateRepository;

	private CertificateTemplateService certificateTemplateService;

	@BeforeEach
	void setUp() {
		certificateTemplateService = new CertificateTemplateService(certificateTemplateRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNewName_succeeds() {
		when(certificateTemplateRepository.existsByNameAndTenantId("Bonafide Certificate", 1L)).thenReturn(false);
		when(certificateTemplateRepository.save(any(CertificateTemplate.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CertificateTemplate template = certificateTemplateService.create("Bonafide Certificate",
				"Certifies {{studentName}}");

		assertEquals("Bonafide Certificate", template.getName());
	}

	@Test
	void create_withDuplicateName_throwsBusinessException() {
		when(certificateTemplateRepository.existsByNameAndTenantId("Bonafide Certificate", 1L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> certificateTemplateService.create("Bonafide Certificate", "body"));
	}

	@Test
	void findByPublicId_withUnknownId_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		when(certificateTemplateRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> certificateTemplateService.findByPublicId(publicId.toString()));
	}

	@Test
	void deactivate_flipsActiveFlagOnResolvedTemplate() {
		CertificateTemplate template = CertificateTemplate.create("Transfer Certificate", "body");
		UUID publicId = UUID.randomUUID();
		template.setPublicId(publicId);
		when(certificateTemplateRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(template));
		when(certificateTemplateRepository.save(any(CertificateTemplate.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CertificateTemplate result = certificateTemplateService.deactivate(publicId.toString());

		assertFalse(result.isActive());
	}
}
