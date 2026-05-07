package com.cms.repository;

import com.cms.entity.ComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplianceReportRepository extends JpaRepository<ComplianceReport, Long> {
    Page<ComplianceReport> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);
    Optional<ComplianceReport> findByUuidAndOrganizationId(String uuid, Long organizationId);
}
