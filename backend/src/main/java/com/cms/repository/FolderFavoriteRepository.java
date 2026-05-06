package com.cms.repository;

import com.cms.entity.FolderFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderFavoriteRepository extends JpaRepository<FolderFavorite, Long> {

    Optional<FolderFavorite> findByUserIdAndFolderId(Long userId, Long folderId);

    List<FolderFavorite> findByUserIdAndFolder_WorkspaceIdOrderByCreatedAtDesc(Long userId, Long workspaceId);

    boolean existsByUserIdAndFolderId(Long userId, Long folderId);
}
