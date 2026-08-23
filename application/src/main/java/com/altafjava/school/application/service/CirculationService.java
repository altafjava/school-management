package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.library.service.LibraryFineCalculator;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * Checkout/return workflow — due-date window and per-day overdue fine rate are tenant-configurable
 * (via {@code TenantSettingOverrideService}, the same scalar-setting mechanism used elsewhere for
 * genuine per-tenant numeric knobs — unlike Board/Curriculum, a fine rate has no internal structure
 * that would justify a first-class entity).
 */
@Service
public class CirculationService {

	static final String DUE_DAYS_SETTING_KEY = "library.checkout.due-days";
	static final String FINE_PER_DAY_RATE_SETTING_KEY = "library.fine.per-day-rate";
	private static final int DEFAULT_DUE_DAYS = 14;
	private static final BigDecimal DEFAULT_FINE_PER_DAY_RATE = BigDecimal.valueOf(5);

	private final CirculationRepository circulationRepository;
	private final BookCopyRepository bookCopyRepository;
	private final StudentRepository studentRepository;
	private final TenantSettingOverrideService tenantSettingOverrideService;
	private final LibraryFineCalculator libraryFineCalculator = new LibraryFineCalculator();

	public CirculationService(CirculationRepository circulationRepository, BookCopyRepository bookCopyRepository,
			StudentRepository studentRepository, TenantSettingOverrideService tenantSettingOverrideService) {
		this.circulationRepository = circulationRepository;
		this.bookCopyRepository = bookCopyRepository;
		this.studentRepository = studentRepository;
		this.tenantSettingOverrideService = tenantSettingOverrideService;
	}

	@Transactional(readOnly = true)
	public Page<Circulation> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return circulationRepository.findAllByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional
	public Circulation checkout(String bookCopyPublicId, String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		BookCopy copy = bookCopyRepository.findByPublicIdAndTenantId(UUID.fromString(bookCopyPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Book copy not found: " + bookCopyPublicId));
		var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));

		copy.checkout();
		bookCopyRepository.save(copy);

		LocalDate today = LocalDate.now();
		LocalDate dueDate = today.plusDays(resolveDueDays(tenantId));
		Circulation circulation = Circulation.checkout(copy.getId(), student.getId(), today, dueDate);
		return circulationRepository.save(circulation);
	}

	@Transactional
	public Circulation returnBook(String circulationPublicId, LocalDate returnedAt) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Circulation circulation = circulationRepository
				.findByPublicIdAndTenantId(UUID.fromString(circulationPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Circulation not found: " + circulationPublicId));

		BigDecimal fine = libraryFineCalculator.calculateFine(circulation.getDueDate(), returnedAt,
				resolveFinePerDayRate(tenantId));
		circulation.returnBook(returnedAt, fine);
		Circulation saved = circulationRepository.save(circulation);

		BookCopy copy = bookCopyRepository.findByIdAndTenantId(circulation.getBookCopyId(), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Book copy not found: " + circulation.getBookCopyId()));
		copy.returnCopy();
		bookCopyRepository.save(copy);

		return saved;
	}

	private int resolveDueDays(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, DUE_DAYS_SETTING_KEY)
				.map(Integer::parseInt)
				.orElse(DEFAULT_DUE_DAYS);
	}

	private BigDecimal resolveFinePerDayRate(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, FINE_PER_DAY_RATE_SETTING_KEY)
				.map(BigDecimal::new)
				.orElse(DEFAULT_FINE_PER_DAY_RATE);
	}
}
