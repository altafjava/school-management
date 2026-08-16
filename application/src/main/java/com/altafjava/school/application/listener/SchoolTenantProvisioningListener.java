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
import lombok.extern.slf4j.Slf4j;

/**
 * Reacts to a new tenant being provisioned by the platform.
 * Seeds school-specific default data: academic year, default fee structure names, grade categories.
 *
 * <p>
 * {@code TenantCreatedEvent} is published via the platform's outbox-backed {@code EventPublisher},
 * whose afterCommit dispatch already only ever runs post-commit — so this is plain
 * {@code @EventListener}, not {@code @TransactionalEventListener}, matching the same reasoning
 * documented on platform-saas's own {@code TenantProvisioningListener}. Runs on
 * {@code platformTaskExecutor} specifically (not a bare {@code @Async}), matching every other
 * platform-aware async listener in this codebase.
 *
 * <p>
 * <b>There is no tenant context to propagate here, by construction.</b> A brand-new tenant is
 * being created — no request thread ever had it "current," so the tenant-context-propagating
 * {@code TaskDecorator} correctly propagates "no tenant," not this one. The fix is not more
 * propagation; it's to bind the new tenant explicitly, for the scope of this handler, from the
 * event payload itself via {@link TenantContext#runAsTenant} — the platform's own sanctioned API
 * for exactly this "system-level code needs to act as tenant X for a bounded scope" case (see its
 * use in {@code AbstractBaseJob}), not a manual {@code TenantContext} mutation of the kind
 * {@code CLAUDE.md}'s hard rule forbids scattering through business logic.
 */
@Slf4j
@Component
public class SchoolTenantProvisioningListener {

	private final AcademicYearRepository academicYearRepository;

	public SchoolTenantProvisioningListener(AcademicYearRepository academicYearRepository) {
		this.academicYearRepository = academicYearRepository;
	}

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
