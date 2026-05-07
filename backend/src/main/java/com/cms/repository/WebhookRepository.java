package com.cms.repository;

import com.cms.entity.Webhook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    Optional<Webhook> findByUuid(String uuid);

    Page<Webhook> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<Webhook> findByOrganizationIdAndStatus(Long organizationId, Webhook.Status status, Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE w.organization.id = :orgId AND w.status = 'ACTIVE'")
    List<Webhook> findActiveByOrganizationId(@Param("orgId") Long organizationId);
}
