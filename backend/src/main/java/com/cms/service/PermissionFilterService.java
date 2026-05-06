package com.cms.service;

import com.cms.dto.permission.EffectivePermissionResponse;
import com.cms.entity.FileEntity;
import com.cms.entity.Folder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionFilterService {

    private final FolderPermissionService folderPermissionService;

    /**
     * Filters folders to only include those the user has at least viewer access to.
     */
    public List<Folder> filterFolders(List<Folder> folders, Long userId) {
        return folders.stream()
                .filter(folder -> hasAccess(userId, folder))
                .collect(Collectors.toList());
    }

    /**
     * Filters files to only include those in folders the user has at least viewer access to.
     */
    public List<FileEntity> filterFiles(List<FileEntity> files, Long userId) {
        return files.stream()
                .filter(file -> hasAccess(userId, file.getFolder()))
                .collect(Collectors.toList());
    }

    /**
     * Check if user has any access to the given folder (at least viewer role).
     */
    public boolean hasAccess(Long userId, Folder folder) {
        EffectivePermissionResponse perm = folderPermissionService.resolveEffectivePermission(userId, folder);
        return perm.getEffectiveRole() != null;
    }

    /**
     * Check if user has at least editor access to the given folder.
     */
    public boolean hasEditorAccess(Long userId, Folder folder) {
        EffectivePermissionResponse perm = folderPermissionService.resolveEffectivePermission(userId, folder);
        String role = perm.getEffectiveRole();
        return "editor".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }

    /**
     * Check if user has admin access to the given folder.
     */
    public boolean hasAdminAccess(Long userId, Folder folder) {
        EffectivePermissionResponse perm = folderPermissionService.resolveEffectivePermission(userId, folder);
        return "admin".equalsIgnoreCase(perm.getEffectiveRole());
    }
}
