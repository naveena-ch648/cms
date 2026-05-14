package com.cms.repository;

import com.cms.entity.SharedLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Links created by the user across all workspaces they belong to, ordered newest first. */
    @Query("SELECT sl FROM SharedLink sl " +
           "JOIN FETCH sl.file " +
           "JOIN FETCH sl.workspace " +
           "JOIN FETCH sl.createdBy " +
           "WHERE sl.createdBy.id = :userId " +
           "AND sl.status = 'ACTIVE' " +
           "AND sl.workspace.id IN :workspaceIds " +
           "ORDER BY sl.createdAt DESC")
    List<SharedLink> findByMeInWorkspaces(@Param("userId") Long userId,
                                          @Param("workspaceIds") List<Long> workspaceIds,
                                          Pageable pageable);

    /** Links created by others (not the current user) in the user's workspaces, ordered newest first. */
    @Query("SELECT sl FROM SharedLink sl " +
           "JOIN FETCH sl.file " +
           "JOIN FETCH sl.workspace " +
           "JOIN FETCH sl.createdBy " +
           "WHERE sl.createdBy.id <> :userId " +
           "AND sl.status = 'ACTIVE' " +
           "AND sl.workspace.id IN :workspaceIds " +
           "ORDER BY sl.createdAt DESC")
    List<SharedLink> findByOthersInWorkspaces(@Param("userId") Long userId,
                                              @Param("workspaceIds") List<Long> workspaceIds,
                                              Pageable pageable);
}
