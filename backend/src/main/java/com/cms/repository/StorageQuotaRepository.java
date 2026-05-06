package com.cms.repository;

import com.cms.entity.StorageQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long> {

    Optional<StorageQuota> findByOrganizationId(Long organizationId);
}
