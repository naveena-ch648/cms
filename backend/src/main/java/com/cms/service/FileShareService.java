package com.cms.service;

import com.cms.dto.fileshare.CreateFileShareRequest;
import com.cms.dto.fileshare.UpdateFileShareRequest;
import com.cms.entity.*;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.FileRepository;
import com.cms.repository.FileShareRepository;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileShareService {

    private final FileShareRepository fileShareRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final PermissionFilterService permissionFilterService;
    private final WorkspaceService workspaceService;

    // ─────────────────────────────────────────────────────────────────────────
    // Share a file with a CMS user
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public FileShare shareFile(String fileUuid, Long sharerUserId, CreateFileShareRequest req) {

        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        if (file.getStatus() != FileEntity.FileStatus.ACTIVE) {
            throw new BusinessRuleException("FILE_NOT_ACTIVE", "Cannot share a trashed or deleted file");
        }

        // Sharer must have editor or admin access (folder-level or workspace-level)
        boolean sharerIsOwner = file.getUploadedBy().getId().equals(sharerUserId);
        boolean sharerHasEditorAccess = permissionFilterService.hasEditorAccess(sharerUserId, file.getFolder())
                || workspaceService.isWorkspaceAdmin(file.getWorkspace().getId(), sharerUserId);

        if (!sharerIsOwner && !sharerHasEditorAccess) {
            throw new BusinessRuleException("INSUFFICIENT_PERMISSION",
                    "You need Editor or Admin access to share this file");
        }

        // Resolve recipient
        User sharedWith = userRepository.findByUuid(req.getSharedWithUserUuid())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getSharedWithUserUuid()));

        // Cannot share with yourself
        if (sharedWith.getId().equals(sharerUserId)) {
            throw new BusinessRuleException("SELF_SHARE", "You cannot share a file with yourself");
        }

        // Recipient must be in the same org
        if (!sharedWith.getOrganization().getId().equals(file.getOrganization().getId())) {
            throw new BusinessRuleException("CROSS_ORG_SHARE",
                    "You can only share files with users in your organisation");
        }

        // Parse permission
        FileShare.SharePermission permission;
        try {
            permission = FileShare.SharePermission.valueOf(req.getPermission().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_PERMISSION",
                    "Permission must be VIEWER or EDITOR");
        }

        // Check for existing share record
        var existing = fileShareRepository.findByFileIdAndSharedWithId(
                file.getId(), sharedWith.getId());

        if (existing.isPresent()) {
            FileShare share = existing.get();
            if (share.getStatus() == FileShare.ShareStatus.ACTIVE && !share.isExpired()) {
                throw new BusinessRuleException("ALREADY_SHARED",
                        "This file is already shared with that user. Update or revoke the existing share.");
            }
            // Re-activate a previously revoked / expired share
            share.setStatus(FileShare.ShareStatus.ACTIVE);
            share.setPermission(permission);
            share.setAllowDownload(req.isAllowDownload());
            share.setWatermarkEnabled(req.isWatermarkEnabled());
            share.setExpiresAt(req.getExpiresAt());
            return fileShareRepository.save(share);
        }

        User sharer = userRepository.findById(sharerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Sharer user not found"));

        FileShare share = FileShare.builder()
                .file(file)
                .sharedBy(sharer)
                .sharedWith(sharedWith)
                .permission(permission)
                .allowDownload(req.isAllowDownload())
                .watermarkEnabled(req.isWatermarkEnabled())
                .expiresAt(req.getExpiresAt())
                .build();

        return fileShareRepository.save(share);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update an existing share (permission / options)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public FileShare updateShare(String shareUuid, Long requestingUserId, UpdateFileShareRequest req) {
        FileShare share = fileShareRepository.findByUuid(shareUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Share not found: " + shareUuid));

        assertCanManageShare(share, requestingUserId);

        if (req.getPermission() != null) {
            try {
                share.setPermission(FileShare.SharePermission.valueOf(req.getPermission().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("INVALID_PERMISSION", "Permission must be VIEWER or EDITOR");
            }
        }
        if (req.getAllowDownload() != null)     share.setAllowDownload(req.getAllowDownload());
        if (req.getWatermarkEnabled() != null)  share.setWatermarkEnabled(req.getWatermarkEnabled());

        if (Boolean.TRUE.equals(req.getRemoveExpiry())) {
            share.setExpiresAt(null);
        } else if (req.getExpiresAt() != null) {
            share.setExpiresAt(req.getExpiresAt());
        }

        return fileShareRepository.save(share);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Revoke a share
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void revokeShare(String shareUuid, Long requestingUserId) {
        FileShare share = fileShareRepository.findByUuid(shareUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Share not found: " + shareUuid));

        assertCanManageShare(share, requestingUserId);

        share.setStatus(FileShare.ShareStatus.REVOKED);
        fileShareRepository.save(share);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List shares for a given file (for the file owner / editor to manage)
    // ─────────────────────────────────────────────────────────────────────────

    public List<FileShare> listSharesForFile(String fileUuid, Long requestingUserId) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        // Must have at least editor access or be the uploader
        boolean owner = file.getUploadedBy().getId().equals(requestingUserId);
        boolean editor = permissionFilterService.hasEditorAccess(requestingUserId, file.getFolder())
                || workspaceService.isWorkspaceAdmin(file.getWorkspace().getId(), requestingUserId);

        if (!owner && !editor) {
            throw new BusinessRuleException("INSUFFICIENT_PERMISSION",
                    "You need Editor access to view shares for this file");
        }

        return fileShareRepository.findActiveByFileId(file.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List files shared WITH the current user (Shared Files page)
    // ─────────────────────────────────────────────────────────────────────────

    public Page<FileShare> listSharedWithMe(Long userId, Pageable pageable) {
        return fileShareRepository.findSharedWithUser(userId, Instant.now(), pageable);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permission check — used by file download / preview APIs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the share if the user has active share access (VIEWER or EDITOR).
     * Throws BusinessRuleException if no valid share exists.
     */
    public FileShare assertShareAccess(String fileUuid, Long userId) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        return fileShareRepository.findActiveShareForUser(file.getId(), userId)
                .orElseThrow(() -> new BusinessRuleException("NO_SHARE_ACCESS",
                        "You do not have shared access to this file"));
    }

    /**
     * Returns true if the user has an active share for the file (any permission level).
     */
    public boolean hasShareAccess(String fileUuid, Long userId) {
        return fileRepository.findByUuid(fileUuid)
                .map(f -> fileShareRepository.existsActiveShareForUser(f.getId(), userId))
                .orElse(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void assertCanManageShare(FileShare share, Long requestingUserId) {
        boolean isSharer  = share.getSharedBy().getId().equals(requestingUserId);
        boolean isOwner   = share.getFile().getUploadedBy().getId().equals(requestingUserId);
        boolean isWsAdmin = workspaceService.isWorkspaceAdmin(
                share.getFile().getWorkspace().getId(), requestingUserId);

        if (!isSharer && !isOwner && !isWsAdmin) {
            throw new BusinessRuleException("INSUFFICIENT_PERMISSION",
                    "Only the sharer, file owner, or workspace admin can modify this share");
        }
    }
}
