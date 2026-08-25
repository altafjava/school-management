package com.altafjava.school.domain.timetable.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

	Page<TimetableEntry> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<TimetableEntry> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<TimetableEntry> findByIdAndTenantId(Long id, Long tenantId);

	List<TimetableEntry> findAllByTenantIdAndClassroomId(Long tenantId, Long classroomId);

	List<TimetableEntry> findAllByTenantIdAndTeacherId(Long tenantId, Long teacherId);

	boolean existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(Long tenantId, DayOfWeek dayOfWeek, Long periodId,
			Long classroomId);

	boolean existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(Long tenantId, DayOfWeek dayOfWeek, Long periodId,
			Long teacherId);
}
