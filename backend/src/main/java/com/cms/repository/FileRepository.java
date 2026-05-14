package com.cms.repository;

import com.cms.entity.FileEntity;
import com.cms.entity.FileEntity.FileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByUuid(String uuid);

    Page<FileEntity> findByFolderIdAndStatus(Long folderId, FileStatus status, Pageable pageable);

    List<FileEntity> findByOrganizationId(Long organizationId);

    List<FileEntity> findByStatusAndPermanentDeleteAtBefore(FileStatus status, Instant cutoff);

    boolean existsByFolderIdAndNameAndStatus(Long folderId, String name, FileStatus status);

    Optional<FileEntity> findByFolderIdAndNameAndStatus(Long folderId, String name, FileStatus status);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileEntity f WHERE f.organization.id = :orgId AND f.status <> 'DELETED'")
    Long sumSizeBytesByOrganizationId(@Param("orgId") Long orgId);

    Page<FileEntity> findByWorkspaceIdAndStatus(Long workspaceId, FileStatus status, Pageable pageable);

    @Query("SELECT f.uuid FROM FileEntity f WHERE f.workspace.uuid = :workspaceUuid AND f.status = 'ACTIVE'")
    List<String> findUuidsByWorkspaceUuid(@Param("workspaceUuid") String workspaceUuid);

    Page<FileEntity> findByWorkspaceIdInAndStatusOrderByLastAccessedAtDesc(List<Long> workspaceIds, FileStatus status, Pageable pageable);

    long countByOrganizationIdAndStatusNot(Long organizationId, FileStatus status);

    @Query("SELECT new map(f.name as name, f.workspace.name as workspaceName, f.updatedAt as updatedAt) " +
           "FROM FileEntity f WHERE f.workspace.id IN :workspaceIds AND f.status = 'ACTIVE' " +
           "AND f.updatedAt >= :since ORDER BY f.updatedAt DESC")
    List<Map<String, Object>> findRecentlyModifiedSummary(
            @Param("workspaceIds") List<Long> workspaceIds,
            @Param("since") Instant since);
}
