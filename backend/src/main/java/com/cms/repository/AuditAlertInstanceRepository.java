package com.cms.repository;

import com.cms.entity.AuditAlertInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditAlertInstanceRepository extends JpaRepository<AuditAlertInstance, Long> {
    Page<AuditAlertInstance> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);
    Page<AuditAlertInstance> findByOrganizationIdAndAcknowledgedOrderByCreatedAtDesc(Long organizationId, Boolean acknowledged, Pageable pageable);
    Page<AuditAlertInstance> findByRuleIdOrderByCreatedAtDesc(Long ruleId, Pageable pageable);
    Optional<AuditAlertInstance> findByUuidAndOrganizationId(String uuid, Long organizationId);
}
