package com.altafjava.school.application.rollup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.organization.OrganizationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.organization.model.Organization;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.rollup.model.AttendanceRollup;
import com.altafjava.school.domain.rollup.model.OrganizationRollupReport;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationRollupServiceTest {

	@Mock
	private OrganizationService organizationService;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;
	@Mock
	private FeePaymentRepository feePaymentRepository;

	private OrganizationRollupService rollupService;

	private final UUID organizationPublicId = UUID.randomUUID();
	private final LocalDate periodStart = LocalDate.of(2026, 1, 1);
	private final LocalDate periodEnd = LocalDate.of(2026, 1, 31);

	@BeforeEach
	void setUp() {
		rollupService = new OrganizationRollupService(organizationService, studentRepository, attendanceRepository,
				feeStructureRepository, feePaymentRepository);
	}

	private Organization organization() {
		Organization organization = Organization.create("Acme School Group", "acme", "contact@acme.test", null);
		organization.setId(10L);
		organization.setPublicId(organizationPublicId);
		return organization;
	}

	private Tenant campus(Long id, String name) {
		return Tenant.builder()
				.id(id)
				.publicId(UUID.randomUUID())
				.name(name)
				.subdomain(name.toLowerCase().replace(" ", "-"))
				.type(TenantType.SHARED)
				.organizationId(10L)
				.build();
	}

	private void mockAttendance(Long tenantId, long present, long absent, long late, long excused) {
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(tenantId), any(), any(),
				eq(AttendanceStatus.PRESENT))).thenReturn(present);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(tenantId), any(), any(),
				eq(AttendanceStatus.ABSENT))).thenReturn(absent);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(tenantId), any(), any(),
				eq(AttendanceStatus.LATE))).thenReturn(late);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(tenantId), any(), any(),
				eq(AttendanceStatus.EXCUSED))).thenReturn(excused);
	}

	@Test
	void generate_aggregatesAcrossAllCampusesInOrganization() {
		Tenant campusA = campus(1L, "Campus A");
		Tenant campusB = campus(2L, "Campus B");

		when(organizationService.findByPublicId(organizationPublicId)).thenReturn(Optional.of(organization()));
		when(organizationService.findTenantsInOrganization(organizationPublicId))
				.thenReturn(List.of(campusA, campusB));

		when(studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L)).thenReturn(100L);
		when(studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 2L)).thenReturn(50L);

		mockAttendance(1L, 90, 5, 3, 2);
		mockAttendance(2L, 40, 5, 5, 0);

		when(feeStructureRepository.findAllByTenantId(1L))
				.thenReturn(List
						.of(FeeStructure.create("Tuition", BigDecimal.valueOf(100), FeeFrequency.MONTHLY, "Standard")));
		when(feeStructureRepository.findAllByTenantId(2L))
				.thenReturn(List
						.of(FeeStructure.create("Tuition", BigDecimal.valueOf(100), FeeFrequency.MONTHLY, "Standard")));
		when(feePaymentRepository.sumPaidAmountByTenantId(1L)).thenReturn(BigDecimal.valueOf(6000));
		when(feePaymentRepository.sumPaidAmountByTenantId(2L)).thenReturn(BigDecimal.valueOf(5500));

		OrganizationRollupReport report = rollupService.generate(organizationPublicId.toString(), periodStart,
				periodEnd);

		assertEquals(2, report.campuses().size());
		assertEquals(150, report.totals().activeStudentCount());
		assertEquals(new AttendanceRollup(130, 10, 8, 2), report.totals().attendance());
		// campusA due = 100 students * 100 = 10000, campusB due = 50 * 100 = 5000 -> total 15000
		assertEquals(BigDecimal.valueOf(15000), report.totals().fees().totalDue());
		assertEquals(BigDecimal.valueOf(11500), report.totals().fees().totalPaid());
		// campusA: due 10000, paid 6000 -> outstanding 4000; campusB: due 5000, paid 5500 ->
		// overpaid 500, clipped at zero before summing (see OrganizationRollupReportTest).
		assertEquals(BigDecimal.valueOf(4000), report.totals().fees().outstandingBalance());
	}

	@Test
	void generate_noCampusesInOrganization_returnsZeroTotals() {
		when(organizationService.findByPublicId(organizationPublicId)).thenReturn(Optional.of(organization()));
		when(organizationService.findTenantsInOrganization(organizationPublicId)).thenReturn(List.of());

		OrganizationRollupReport report = rollupService.generate(organizationPublicId.toString(), periodStart, periodEnd);

		assertEquals(0, report.campuses().size());
		assertEquals(0, report.totals().activeStudentCount());
		assertEquals(AttendanceRollup.ZERO, report.totals().attendance());
	}

	@Test
	void generate_organizationNotFound_throwsResourceNotFoundException() {
		when(organizationService.findByPublicId(organizationPublicId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> rollupService.generate(organizationPublicId.toString(), periodStart, periodEnd));
	}

	@Test
	void generate_toBeforeFrom_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> rollupService.generate(organizationPublicId.toString(), periodEnd, periodStart));
	}

	@Test
	void generate_rangeExceedsMaximum_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> rollupService.generate(organizationPublicId.toString(), LocalDate.of(2020, 1, 1),
						LocalDate.of(2026, 1, 1)));
	}
}
