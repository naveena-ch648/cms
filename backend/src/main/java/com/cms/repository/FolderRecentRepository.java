package com.cms.repository;

import com.cms.entity.FolderRecent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRecentRepository extends JpaRepository<FolderRecent, Long> {

    Optional<FolderRecent> findByUserIdAndFolderId(Long userId, Long folderId);

    List<FolderRecent> findTop10ByUserIdAndFolder_WorkspaceIdOrderByAccessedAtDesc(Long userId, Long workspaceId);

    long countByUserIdAndFolder_WorkspaceId(Long userId, Long workspaceId);

    FolderRecent findFirstByUserIdAndFolder_WorkspaceIdOrderByAccessedAtAsc(Long userId, Long workspaceId);
}
