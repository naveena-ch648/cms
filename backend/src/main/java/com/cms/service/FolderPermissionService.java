package com.cms.service;

import com.cms.dto.folder.FolderPermissionResponse;
import com.cms.dto.permission.EffectivePermissionResponse;
import com.cms.entity.*;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderPermissionService {

    private final FolderPermissionRepository folderPermissionRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    private static final String PERM_CACHE_PREFIX = "folder_perm:";
    private static final long PERM_CACHE_TTL_MINUTES = 5;

    @Transactional
    public FolderPermission assignPermission(String folderUuid, String userUuid, String groupUuid, String roleUuid) {
        Folder folder = folderRepository.findByUuid(folderUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        Role role = roleRepository.findByUuid(roleUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (userUuid == null && groupUuid == null) {
            throw new BusinessRuleException("INVALID_PERMISSION", "Either userId or groupId must be specified");
        }
        if (userUuid != null && groupUuid != null) {
            throw new BusinessRuleException("INVALID_PERMISSION", "Only one of userId or groupId can be specified");
        }

        User user = null;
        Group group = null;

        if (userUuid != null) {
            user = userRepository.findByUuid(userUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            // Check for existing permission
            Optional<FolderPermission> existing = folderPermissionRepository
                    .findByFolderIdAndUserId(folder.getId(), user.getId());
            if (existing.isPresent()) {
                FolderPermission fp = existing.get();
                fp.setRole(role);
                fp = folderPermissionRepository.save(fp);
                invalidatePermCache(user.getId(), folder.getId());
                return fp;
            }
        }

        if (groupUuid != null) {
            group = groupRepository.findByUuid(groupUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
            Optional<FolderPermission> existing = folderPermissionRepository
                    .findByFolderIdAndGroupId(folder.getId(), group.getId());
            if (existing.isPresent()) {
                FolderPermission fp = existing.get();
                fp.setRole(role);
                fp = folderPermissionRepository.save(fp);
                return fp;
            }
        }

        FolderPermission fp = FolderPermission.builder()
                .folder(folder)
                .user(user)
                .group(group)
                .role(role)
                .build();

        fp = folderPermissionRepository.save(fp);

        if (user != null) {
            invalidatePermCache(user.getId(), folder.getId());
        }

        auditService.log(folder.getWorkspace().getOrganization(), user, "FOLDER_PERMISSION_ASSIGNED",
                "FolderPermission", fp.getId(),
                "Assigned role " + role.getName() + " on folder " + folder.getName(), null);

        return fp;
    }

    @Transactional
    public void removePermission(Long permissionId) {
        FolderPermission fp = folderPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        if (fp.getUser() != null) {
            invalidatePermCache(fp.getUser().getId(), fp.getFolder().getId());
        }
        auditService.log(fp.getFolder().getWorkspace().getOrganization(), fp.getUser(),
                "FOLDER_PERMISSION_REMOVED", "FolderPermission", fp.getId(),
                "Removed permission on folder " + fp.getFolder().getName(), null);
        folderPermissionRepository.delete(fp);
    }

    public String getEffectiveRole(Long userId, Long folderId) {
        // Walk up ancestors looking for explicit permission
        Folder current = folderRepository.findById(folderId).orElse(null);
        while (current != null) {
            Optional<FolderPermission> perm = folderPermissionRepository
                    .findByFolderIdAndUserId(current.getId(), userId);
            if (perm.isPresent()) {
                return perm.get().getRole().getName();
            }
            current = current.getParent();
        }
        return null;
    }

    public List<FolderPermissionResponse> listPermissions(String folderUuid) {
        Folder folder = folderRepository.findByUuid(folderUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        List<FolderPermissionResponse> results = new ArrayList<>();

        // Explicit permissions on this folder
        List<FolderPermission> explicit = folderPermissionRepository.findByFolderId(folder.getId());
        for (FolderPermission fp : explicit) {
            results.add(FolderPermissionResponse.from(fp));
        }

        // Walk up ancestors for inherited permissions
        Folder parent = folder.getParent();
        while (parent != null) {
            List<FolderPermission> inherited = folderPermissionRepository.findByFolderId(parent.getId());
            for (FolderPermission fp : inherited) {
                // Only include if not already overridden by explicit permission
                boolean overridden = results.stream().anyMatch(r ->
                        (r.getUserId() != null && r.getUserId().equals(
                                fp.getUser() != null ? fp.getUser().getUuid() : null)) ||
                        (r.getGroupId() != null && r.getGroupId().equals(
                                fp.getGroup() != null ? fp.getGroup().getUuid() : null))
                );
                if (!overridden) {
                    results.add(FolderPermissionResponse.inherited(fp, parent.getUuid()));
                }
            }
            parent = parent.getParent();
        }

        return results;
    }

    public EffectivePermissionResponse resolveEffectivePermission(Long userId, Folder folder) {
        if (folder == null) {
            return EffectivePermissionResponse.builder().effectiveRole(null).build();
        }

        Folder current = folder;
        while (current != null) {
            Optional<FolderPermission> perm = folderPermissionRepository
                    .findByFolderIdAndUserId(current.getId(), userId);
            if (perm.isPresent()) {
                String source = current.getId().equals(folder.getId()) ? "DIRECT" : "INHERITED";
                return EffectivePermissionResponse.builder()
                        .folderUuid(folder.getUuid())
                        .effectiveRole(perm.get().getRole().getName())
                        .source(source)
                        .sourceFolderUuid(current.getUuid())
                        .build();
            }
            current = current.getParent();
        }

        return EffectivePermissionResponse.builder()
                .folderUuid(folder.getUuid())
                .effectiveRole(null)
                .build();
    }

    private void invalidatePermCache(Long userId, Long folderId) {
        // No-op: caching removed
    }
}
