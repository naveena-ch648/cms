package com.cms.repository;

import com.cms.entity.FolderPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderPermissionRepository extends JpaRepository<FolderPermission, Long> {

    Optional<FolderPermission> findByFolderIdAndUserId(Long folderId, Long userId);

    Optional<FolderPermission> findByFolderIdAndGroupId(Long folderId, Long groupId);

    List<FolderPermission> findByFolderId(Long folderId);

    List<FolderPermission> findByUserId(Long userId);

    void deleteByFolderId(Long folderId);
}
