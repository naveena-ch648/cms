package com.cms.repository;

import com.cms.entity.AuditAlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditAlertRuleRepository extends JpaRepository<AuditAlertRule, Long> {
    List<AuditAlertRule> findByOrganizationIdAndEnabledTrue(Long organizationId);
    List<AuditAlertRule> findByOrganizationId(Long organizationId);
    Optional<AuditAlertRule> findByUuidAndOrganizationId(String uuid, Long organizationId);
}
