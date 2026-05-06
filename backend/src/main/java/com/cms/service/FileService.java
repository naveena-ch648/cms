package com.cms.service;

import com.cms.entity.FileEntity;
import com.cms.entity.FileEntity.FileStatus;
import com.cms.entity.FileVersion;
import com.cms.entity.Folder;
import com.cms.entity.StorageQuota;
import com.cms.entity.User;
import com.cms.event.FileIndexEventPublisher;
import com.cms.repository.FileRepository;
import com.cms.repository.FileVersionRepository;
import com.cms.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final FolderRepository folderRepository;
    private final StorageService storageService;
    private final StorageQuotaService storageQuotaService;
    private final FileProcessingQueueService fileProcessingQueueService;
    private final AuditService auditService;
    private final PreviewService previewService;
    private final FileIndexEventPublisher fileIndexEventPublisher;

    public FileEntity getByUuid(String uuid) {
        return fileRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + uuid));
    }

    public Page<FileEntity> listByFolder(Long folderId, FileStatus status, Pageable pageable) {
        return fileRepository.findByFolderIdAndStatus(folderId, status, pageable);
    }

    public Page<FileEntity> listTrash(Long workspaceId, Pageable pageable) {
        return fileRepository.findByWorkspaceIdAndStatus(workspaceId, FileStatus.TRASHED, pageable);
    }

    @Transactional
    public FileEntity createFileRecord(Folder folder, User uploader, String name, String originalName,
                                       long sizeBytes, String mimeType, String storageKey,
                                       String storageBucket, String description, String tags,
                                       String onDuplicate) {
        String resolvedName = handleDuplicate(folder.getId(), name, onDuplicate);

        FileEntity file = FileEntity.builder()
                .folder(folder)
                .organization(folder.getWorkspace().getOrganization())
                .workspace(folder.getWorkspace())
                .name(resolvedName)
                .originalName(originalName)
                .sizeBytes(sizeBytes)
                .mimeType(mimeType)
                .storageKey(storageKey)
                .storageBucket(storageBucket)
                .description(description)
                .tags(tags)
                .uploadedBy(uploader)
                .uploadCompletedAt(Instant.now())
                .build();

        file = fileRepository.save(file);

        // Create version 1 record
        FileVersion version1 = FileVersion.builder()
                .file(file)
                .versionNumber(1)
                .storageKey(storageKey)
                .storageBucket(storageBucket)
                .sizeBytes(sizeBytes)
                .mimeType(mimeType)
                .changeNote("Initial upload")
                .uploadedBy(uploader)
                .build();
        version1 = fileVersionRepository.save(version1);
        file.setCurrentVersion(version1);
        file.setVersionCount(1);
        file = fileRepository.save(file);

        storageQuotaService.updateUsedStorage(folder.getWorkspace().getOrganization().getId(), sizeBytes);
        fileProcessingQueueService.publishJob(file.getId(), folder.getWorkspace().getOrganization().getId(), "process");

        // Dispatch thumbnail generation for preview system
        try {
            previewService.dispatchThumbnailJob(file);
        } catch (Exception e) {
            log.warn("Failed to dispatch thumbnail job for file {}: {}", file.getUuid(), e.getMessage());
        }

        // Publish search index event
        try {
            fileIndexEventPublisher.publishIndexEvent(
                    file.getUuid(),
                    file.getWorkspace().getUuid(),
                    file.getOrganization().getUuid());
        } catch (Exception e) {
            log.warn("Failed to publish search index event for file {}: {}", file.getUuid(), e.getMessage());
        }

        return file;
    }

    @Transactional
    public FileEntity updateFile(String fileUuid, String newName, String description, String tags, User user) {
        FileEntity file = getByUuid(fileUuid);

        if (newName != null && !newName.isBlank() && !newName.equals(file.getName())) {
            if (fileRepository.existsByFolderIdAndNameAndStatus(file.getFolder().getId(), newName, FileStatus.ACTIVE)) {
                throw new IllegalArgumentException("A file named '" + newName + "' already exists in this folder");
            }
            file.setName(newName);
        }
        if (description != null) {
            file.setDescription(description);
        }
        if (tags != null) {
            file.setTags(tags);
        }

        return fileRepository.save(file);
    }

    @Transactional
    public FileEntity moveFile(String fileUuid, String targetFolderUuid, String onDuplicate, User user) {
        FileEntity file = getByUuid(fileUuid);
        Folder targetFolder = folderRepository.findByUuid(targetFolderUuid)
                .orElseThrow(() -> new IllegalArgumentException("Target folder not found: " + targetFolderUuid));

        String resolvedName = handleDuplicate(targetFolder.getId(), file.getName(), onDuplicate);
        file.setFolder(targetFolder);
        file.setName(resolvedName);

        return fileRepository.save(file);
    }

    @Transactional
    public FileEntity copyFile(String fileUuid, String targetFolderUuid, String onDuplicate, User user) {
        FileEntity source = getByUuid(fileUuid);
        Folder targetFolder = folderRepository.findByUuid(targetFolderUuid)
                .orElseThrow(() -> new IllegalArgumentException("Target folder not found: " + targetFolderUuid));

        String resolvedName = handleDuplicate(targetFolder.getId(), source.getName(), onDuplicate);

        // Copy object in MinIO
        String newStorageKey = source.getStorageKey().replace(source.getUuid(), java.util.UUID.randomUUID().toString());
        storageService.copyObject(source.getStorageBucket(), source.getStorageKey(),
                source.getStorageBucket(), newStorageKey);

        FileEntity copy = FileEntity.builder()
                .folder(targetFolder)
                .organization(source.getOrganization())
                .workspace(source.getWorkspace())
                .name(resolvedName)
                .originalName(source.getOriginalName())
                .sizeBytes(source.getSizeBytes())
                .mimeType(source.getMimeType())
                .storageKey(newStorageKey)
                .storageBucket(source.getStorageBucket())
                .checksumSha256(source.getChecksumSha256())
                .description(source.getDescription())
                .tags(source.getTags())
                .uploadedBy(user)
                .uploadCompletedAt(Instant.now())
                .build();

        copy = fileRepository.save(copy);
        storageQuotaService.updateUsedStorage(source.getOrganization().getId(), source.getSizeBytes());

        return copy;
    }

    @Transactional
    public FileEntity trashFile(String fileUuid, User user) {
        FileEntity file = getByUuid(fileUuid);
        StorageQuota quota = storageQuotaService.getQuotaForOrg(file.getOrganization().getId());

        file.setStatus(FileStatus.TRASHED);
        file.setTrashedAt(Instant.now());
        file.setTrashedBy(user);
        file.setPermanentDeleteAt(Instant.now().plus(quota.getTrashRetentionDays(), ChronoUnit.DAYS));

        return fileRepository.save(file);
    }

    @Transactional
    public FileEntity restoreFile(String fileUuid) {
        FileEntity file = getByUuid(fileUuid);
        if (file.getStatus() != FileStatus.TRASHED) {
            throw new IllegalStateException("File is not in trash");
        }

        file.setStatus(FileStatus.ACTIVE);
        file.setTrashedAt(null);
        file.setTrashedBy(null);
        file.setPermanentDeleteAt(null);

        return fileRepository.save(file);
    }

    @Transactional
    public void permanentDelete(String fileUuid) {
        FileEntity file = getByUuid(fileUuid);

        // Publish search delete event before removing
        try {
            fileIndexEventPublisher.publishDeleteEvent(
                    file.getUuid(),
                    file.getWorkspace().getUuid(),
                    file.getOrganization().getUuid());
        } catch (Exception e) {
            log.warn("Failed to publish search delete event for file {}: {}", file.getUuid(), e.getMessage());
        }

        // Delete all version storage objects
        var versions = fileVersionRepository.findByFileIdOrderByVersionNumberAsc(file.getId());
        long totalVersionSize = 0;
        for (FileVersion version : versions) {
            storageService.deleteObject(version.getStorageBucket(), version.getStorageKey());
            totalVersionSize += version.getSizeBytes();
        }
        fileVersionRepository.deleteAllByFileId(file.getId());

        // Delete main storage object (if different from version keys)
        storageService.deleteObject(file.getStorageBucket(), file.getStorageKey());
        if (file.getThumbnailKey() != null) {
            storageService.deleteObject(file.getStorageBucket(), file.getThumbnailKey());
        }

        storageQuotaService.updateUsedStorage(file.getOrganization().getId(), -file.getSizeBytes());
        fileRepository.delete(file);
    }

    @Transactional
    public void incrementDownloadCount(String fileUuid) {
        FileEntity file = getByUuid(fileUuid);
        file.setDownloadCount(file.getDownloadCount() + 1);
        file.setLastAccessedAt(Instant.now());
        fileRepository.save(file);
    }

    private String handleDuplicate(Long folderId, String name, String strategy) {
        if (strategy == null) strategy = "rename";

        if (!fileRepository.existsByFolderIdAndNameAndStatus(folderId, name, FileStatus.ACTIVE)) {
            return name;
        }

        switch (strategy) {
            case "error":
                throw new IllegalArgumentException("A file named '" + name + "' already exists in this folder");
            case "replace":
                FileEntity existing = fileRepository.findByFolderIdAndNameAndStatus(folderId, name, FileStatus.ACTIVE)
                        .orElse(null);
                if (existing != null) {
                    storageService.deleteObject(existing.getStorageBucket(), existing.getStorageKey());
                    storageQuotaService.updateUsedStorage(existing.getOrganization().getId(), -existing.getSizeBytes());
                    fileRepository.delete(existing);
                }
                return name;
            case "rename":
            default:
                return generateUniqueName(folderId, name);
        }
    }

    private String generateUniqueName(Long folderId, String name) {
        String baseName = name;
        String extension = "";
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            baseName = name.substring(0, lastDot);
            extension = name.substring(lastDot);
        }

        int counter = 1;
        String candidate;
        do {
            candidate = baseName + " (" + counter + ")" + extension;
            counter++;
        } while (fileRepository.existsByFolderIdAndNameAndStatus(folderId, candidate, FileStatus.ACTIVE));

        return candidate;
    }

    private com.cms.entity.StorageQuota getQuota(Long orgId) {
        return storageQuotaService.getQuotaForOrg(orgId);
    }
}
