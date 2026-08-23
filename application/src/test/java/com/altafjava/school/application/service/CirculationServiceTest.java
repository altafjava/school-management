package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.BookCopyStatus;
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

	private static final UUID COPY_PUBLIC_ID = UUID.randomUUID();
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private CirculationRepository circulationRepository;
	@Mock
	private BookCopyRepository bookCopyRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TenantSettingOverrideService tenantSettingOverrideService;

	private CirculationService circulationService;

	@BeforeEach
	void setUp() {
		circulationService = new CirculationService(circulationRepository, bookCopyRepository, studentRepository,
				tenantSettingOverrideService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private BookCopy copyWithId(long id) {
		BookCopy copy = BookCopy.create(1L, "COPY-1");
		copy.setId(id);
		return copy;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void checkout_withNoTenantSetting_usesDefaultDueDays() {
		BookCopy copy = copyWithId(5L);
		when(bookCopyRepository.findByPublicIdAndTenantId(COPY_PUBLIC_ID, 1L)).thenReturn(Optional.of(copy));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(tenantSettingOverrideService.get(1L, CirculationService.DUE_DAYS_SETTING_KEY))
				.thenReturn(Optional.empty());
		when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(inv -> inv.getArgument(0));
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));

		Circulation circulation = circulationService.checkout(COPY_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString());

		assertEquals(BookCopyStatus.CHECKED_OUT, copy.getStatus());
		assertEquals(circulation.getCheckedOutAt().plusDays(14), circulation.getDueDate());
	}

	@Test
	void checkout_withTenantConfiguredDueDays_usesConfiguredValue() {
		BookCopy copy = copyWithId(5L);
		when(bookCopyRepository.findByPublicIdAndTenantId(COPY_PUBLIC_ID, 1L)).thenReturn(Optional.of(copy));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(tenantSettingOverrideService.get(1L, CirculationService.DUE_DAYS_SETTING_KEY))
				.thenReturn(Optional.of("21"));
		when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(inv -> inv.getArgument(0));
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));

		Circulation circulation = circulationService.checkout(COPY_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString());

		assertEquals(circulation.getCheckedOutAt().plusDays(21), circulation.getDueDate());
	}

	@Test
	void returnBook_overdue_calculatesFineUsingConfiguredRate() {
		UUID circulationPublicId = UUID.randomUUID();
		Circulation circulation = Circulation.checkout(5L, 10L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));
		BookCopy copy = copyWithId(5L);
		copy.checkout();
		when(circulationRepository.findByPublicIdAndTenantId(circulationPublicId, 1L))
				.thenReturn(Optional.of(circulation));
		when(tenantSettingOverrideService.get(1L, CirculationService.FINE_PER_DAY_RATE_SETTING_KEY))
				.thenReturn(Optional.of("10"));
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));
		when(bookCopyRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(copy));
		when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(inv -> inv.getArgument(0));

		Circulation returned = circulationService.returnBook(circulationPublicId.toString(),
				LocalDate.of(2026, 4, 20));

		assertEquals(0, BigDecimal.valueOf(50).compareTo(returned.getFineAmount()));
		assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
	}
}
