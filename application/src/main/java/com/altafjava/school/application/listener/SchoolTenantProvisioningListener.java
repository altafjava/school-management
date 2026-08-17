package com.altafjava.school.application.listener;

import java.time.LocalDate;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.events.TenantCreatedEvent;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantContextSnapshot;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reacts to a new tenant being provisioned by the platform. Seeds school-specific default data
 * (currently: academic year). Role seeding is not needed here — TEACHER/STUDENT/PARENT are global
 * system-role templates seeded once via Liquibase, see {@code Role.java}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchoolTenantProvisioningListener {

	private final AcademicYearRepository academicYearRepository;

	@Async("platformTaskExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@EventListener
	public void onTenantCreated(TenantCreatedEvent event) {
		log.info("action=school-tenant-provisioning tenantId={} tenantType={}", event.tenantId(), event.tenantType());
		TenantContextSnapshot snapshot = new TenantContextSnapshot(
				event.tenantId(), null, null, event.tenantType(), null);
		TenantContext.runAsTenant(snapshot, () -> seedDefaultAcademicYear(event.tenantId()));
		log.info("action=school-tenant-provisioning-complete tenantId={}", event.tenantId());
	}

	private void seedDefaultAcademicYear(Long tenantId) {
		LocalDate now = LocalDate.now();
		int year = now.getYear();
		String name = year + "-" + (year + 1);

		if (academicYearRepository.existsByNameAndTenantId(name, tenantId)) {
			log.info("action=seed-academic-year-skipped tenantId={} name={} reason=already-exists", tenantId, name);
			return;
		}

		AcademicYear academicYear = AcademicYear.create(
				name,
				LocalDate.of(year, 4, 1),
				LocalDate.of(year + 1, 3, 31),
				true);
		academicYearRepository.save(academicYear);
		log.info("action=seed-academic-year-created tenantId={} name={}", tenantId, name);
	}
}
