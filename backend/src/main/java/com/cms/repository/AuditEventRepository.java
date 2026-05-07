package com.cms.repository;

import com.cms.entity.AuditCategory;
import com.cms.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<AuditEvent> findByOrganizationIdAndEventType(Long organizationId, String eventType, Pageable pageable);
    Page<AuditEvent> findByOrganizationIdAndCategory(Long organizationId, AuditCategory category, Pageable pageable);
    Page<AuditEvent> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(String resourceType, Long resourceId, Pageable pageable);

    Optional<AuditEvent> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("SELECT e FROM AuditEvent e WHERE e.organization.id = :orgId AND e.createdAt BETWEEN :from AND :to ORDER BY e.createdAt DESC")
    Page<AuditEvent> findByOrganizationIdAndDateRange(@Param("orgId") Long orgId, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("SELECT COUNT(e) FROM AuditEvent e WHERE e.organization.id = :orgId AND e.createdAt BETWEEN :from AND :to")
    long countByOrganizationIdAndDateRange(@Param("orgId") Long orgId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT e.category, COUNT(e) FROM AuditEvent e WHERE e.organization.id = :orgId AND e.createdAt BETWEEN :from AND :to GROUP BY e.category")
    List<Object[]> countByOrganizationIdAndDateRangeGroupByCategory(@Param("orgId") Long orgId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT e.outcome, COUNT(e) FROM AuditEvent e WHERE e.organization.id = :orgId AND e.createdAt BETWEEN :from AND :to GROUP BY e.outcome")
    List<Object[]> countByOrganizationIdAndDateRangeGroupByOutcome(@Param("orgId") Long orgId, @Param("from") Instant from, @Param("to") Instant to);

    @Modifying
    @Query("DELETE FROM AuditEvent e WHERE e.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);

    List<AuditEvent> findByOrganizationIdAndEventTypeAndUserIdAndCreatedAtAfter(
            Long organizationId, String eventType, Long userId, Instant after);
}
