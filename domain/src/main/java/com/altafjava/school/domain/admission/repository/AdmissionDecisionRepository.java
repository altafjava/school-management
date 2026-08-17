package com.altafjava.school.domain.admission.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.admission.model.AdmissionDecision;

public interface AdmissionDecisionRepository extends JpaRepository<AdmissionDecision, Long> {

	@Query("SELECT d FROM AdmissionDecision d WHERE d.tenantId = :tenantId AND d.admissionId = :admissionId")
	List<AdmissionDecision> findByAdmissionId(@Param("tenantId") Long tenantId,
			@Param("admissionId") Long admissionId);
}
