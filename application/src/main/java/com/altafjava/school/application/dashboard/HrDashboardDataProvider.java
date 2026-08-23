package com.altafjava.school.application.dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.report.provider.ReportDataProvider;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/** HR summary: one aggregate row, computed from single COUNT queries. */
@Component
public class HrDashboardDataProvider implements ReportDataProvider {

	private final TeacherRepository teacherRepository;
	private final DepartmentRepository departmentRepository;
	private final LeaveRequestRepository leaveRequestRepository;

	public HrDashboardDataProvider(TeacherRepository teacherRepository, DepartmentRepository departmentRepository,
			LeaveRequestRepository leaveRequestRepository) {
		this.teacherRepository = teacherRepository;
		this.departmentRepository = departmentRepository;
		this.leaveRequestRepository = leaveRequestRepository;
	}

	@Override
	public List<Map<String, Object>> fetchData(Map<String, Object> parameters) {
		Long tenantId = TenantContext.getCurrentTenantId();

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("teacherCount", teacherRepository.countByTenantId(tenantId));
		row.put("departmentCount", departmentRepository.countByTenantId(tenantId));
		row.put("pendingLeaveRequestCount",
				leaveRequestRepository.countByTenantIdAndStatus(tenantId, LeaveRequestStatus.PENDING));
		return List.of(row);
	}
}
