package com.cms.repository;

import com.cms.entity.SharedLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharedLinkRepository extends JpaRepository<SharedLink, Long> {

    Optional<SharedLink> findByToken(String token);

    Optional<SharedLink> findByUuid(String uuid);

    Page<SharedLink> findByCreatedByIdAndWorkspaceId(Long createdById, Long workspaceId, Pageable pageable);

    Page<SharedLink> findByCreatedByIdAndWorkspaceIdAndStatus(Long createdById, Long workspaceId, SharedLink.LinkStatus status, Pageable pageable);

    Page<SharedLink> findByWorkspaceId(Long workspaceId, Pageable pageable);

    Page<SharedLink> findByWorkspaceIdAndStatus(Long workspaceId, SharedLink.LinkStatus status, Pageable pageable);

    List<SharedLink> findByStatusAndExpiresAtBefore(SharedLink.LinkStatus status, java.time.Instant before);
}
