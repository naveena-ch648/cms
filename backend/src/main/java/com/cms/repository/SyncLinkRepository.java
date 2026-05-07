package com.cms.repository;

import com.cms.entity.SyncLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SyncLinkRepository extends JpaRepository<SyncLink, Long> {

    Optional<SyncLink> findByUuid(String uuid);

    List<SyncLink> findByOrganizationId(Long organizationId);

    List<SyncLink> findByOrganizationIdAndStatus(Long organizationId, SyncLink.Status status);

    Optional<SyncLink> findByFolderId(Long folderId);

    List<SyncLink> findByStatusAndNextSyncAtBefore(SyncLink.Status status, Instant now);
}
