package com.cms.repository;

import com.cms.entity.IntegrationConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, Long> {

    Optional<IntegrationConnection> findByUuid(String uuid);

    List<IntegrationConnection> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    Optional<IntegrationConnection> findByOrganizationIdAndUserIdAndProvider(Long organizationId, Long userId, String provider);

    List<IntegrationConnection> findByOrganizationIdAndStatus(Long organizationId, IntegrationConnection.Status status);
}
