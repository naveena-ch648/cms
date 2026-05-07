package com.cms.service;

import com.cms.entity.FileEntity;
import com.cms.entity.FileVersion;
import com.cms.entity.User;
import com.cms.repository.FileVersionRepository;
import com.cms.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileVersionService {

    private final FileVersionRepository fileVersionRepository;
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final StorageQuotaService storageQuotaService;
    private final AuditService auditService;
    private final EmbeddingJobService embeddingJobService;

    @Transactional
    public FileVersion uploadNewVersion(String fileUuid, MultipartFile file, String changeNote, User uploader) {
        FileEntity fileEntity = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileUuid));

        // Determine next version number
        int nextVersion = fileEntity.getVersionCount() + 1;

        // Upload to MinIO with version-specific key
        String versionStorageKey = buildVersionStorageKey(fileEntity, nextVersion, fileEntity.getName());
        try {
            storageService.putObject(
                    fileEntity.getStorageBucket(),
                    versionStorageKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload version content", e);
        }

        // Create version record
        FileVersion version = FileVersion.builder()
                .file(fileEntity)
                .versionNumber(nextVersion)
                .storageKey(versionStorageKey)
                .storageBucket(fileEntity.getStorageBucket())
                .sizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .changeNote(changeNote)
                .uploadedBy(uploader)
                .build();
        version = fileVersionRepository.save(version);

        // Update file to point to new version
        fileEntity.setCurrentVersion(version);
        fileEntity.setVersionCount(nextVersion);
        fileEntity.setSizeBytes(file.getSize());
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setStorageKey(versionStorageKey);
        fileRepository.save(fileEntity);

        // Update storage quota
        storageQuotaService.updateUsedStorage(fileEntity.getOrganization().getId(), file.getSize());

        // Audit log
        auditService.log(fileEntity.getOrganization(), uploader, "UPLOAD_VERSION",
                "FILE", fileEntity.getId(), "version=" + nextVersion, null);

        // Re-trigger embedding for updated content
        try {
            embeddingJobService.retriggerEmbedding(fileEntity);
        } catch (Exception e) {
            log.warn("Failed to re-trigger embedding for file {}: {}", fileUuid, e.getMessage());
        }

        log.info("Created version {} for file {}", nextVersion, fileUuid);
        return version;
    }

    public Page<FileVersion> listVersions(String fileUuid, Pageable pageable) {
        FileEntity fileEntity = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileUuid));
        return fileVersionRepository.findByFileIdOrderByVersionNumberDesc(fileEntity.getId(), pageable);
    }

    public FileVersion getVersion(String fileUuid, String versionUuid) {
        FileEntity fileEntity = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileUuid));
        return fileVersionRepository.findByFileIdAndUuid(fileEntity.getId(), versionUuid)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionUuid));
    }

    public String getVersionDownloadUrl(String fileUuid, String versionUuid) {
        FileVersion version = getVersion(fileUuid, versionUuid);
        return storageService.presignGetUrl(version.getStorageBucket(), version.getStorageKey(), Duration.ofHours(1));
    }

    @Transactional
    public FileVersion restoreVersion(String fileUuid, String versionUuid, User user) {
        FileEntity fileEntity = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileUuid));
        FileVersion sourceVersion = fileVersionRepository.findByFileIdAndUuid(fileEntity.getId(), versionUuid)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionUuid));

        int nextVersion = fileEntity.getVersionCount() + 1;

        // Copy the source version's object to a new key
        String newStorageKey = buildVersionStorageKey(fileEntity, nextVersion, fileEntity.getName());
        storageService.copyObject(
                sourceVersion.getStorageBucket(), sourceVersion.getStorageKey(),
                sourceVersion.getStorageBucket(), newStorageKey
        );

        // Create new version record
        FileVersion restoredVersion = FileVersion.builder()
                .file(fileEntity)
                .versionNumber(nextVersion)
                .storageKey(newStorageKey)
                .storageBucket(sourceVersion.getStorageBucket())
                .sizeBytes(sourceVersion.getSizeBytes())
                .mimeType(sourceVersion.getMimeType())
                .checksumSha256(sourceVersion.getChecksumSha256())
                .changeNote("Restored from version " + sourceVersion.getVersionNumber())
                .uploadedBy(user)
                .build();
        restoredVersion = fileVersionRepository.save(restoredVersion);

        // Update file to point to restored version
        fileEntity.setCurrentVersion(restoredVersion);
        fileEntity.setVersionCount(nextVersion);
        fileEntity.setSizeBytes(sourceVersion.getSizeBytes());
        fileEntity.setMimeType(sourceVersion.getMimeType());
        fileEntity.setStorageKey(newStorageKey);
        fileRepository.save(fileEntity);

        // Update storage quota (new copy uses storage)
        storageQuotaService.updateUsedStorage(fileEntity.getOrganization().getId(), sourceVersion.getSizeBytes());

        // Audit log
        auditService.log(fileEntity.getOrganization(), user, "RESTORE_VERSION",
                "FILE", fileEntity.getId(),
                "restored_from=" + sourceVersion.getVersionNumber() + ",new_version=" + nextVersion, null);

        log.info("Restored version {} to new version {} for file {}", sourceVersion.getVersionNumber(), nextVersion, fileUuid);
        return restoredVersion;
    }

    public Map<String, Object> compareVersions(String fileUuid, String versionUuid1, String versionUuid2) {
        FileEntity fileEntity = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileUuid));

        FileVersion v1 = fileVersionRepository.findByFileIdAndUuid(fileEntity.getId(), versionUuid1)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionUuid1));
        FileVersion v2 = fileVersionRepository.findByFileIdAndUuid(fileEntity.getId(), versionUuid2)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionUuid2));

        String downloadUrl1 = storageService.presignGetUrl(v1.getStorageBucket(), v1.getStorageKey(), Duration.ofHours(1));
        String downloadUrl2 = storageService.presignGetUrl(v2.getStorageBucket(), v2.getStorageKey(), Duration.ofHours(1));

        boolean sameContent = v1.getChecksumSha256() != null && v2.getChecksumSha256() != null
                && v1.getChecksumSha256().equals(v2.getChecksumSha256());

        return Map.of(
                "version1", Map.of(
                        "id", v1.getUuid(),
                        "versionNumber", v1.getVersionNumber(),
                        "sizeBytes", v1.getSizeBytes(),
                        "mimeType", v1.getMimeType(),
                        "checksumSha256", v1.getChecksumSha256() != null ? v1.getChecksumSha256() : "",
                        "uploadedBy", Map.of("id", v1.getUploadedBy().getUuid(), "name", v1.getUploadedBy().getFirstName() + " " + v1.getUploadedBy().getLastName()),
                        "createdAt", v1.getCreatedAt().toString(),
                        "downloadUrl", downloadUrl1
                ),
                "version2", Map.of(
                        "id", v2.getUuid(),
                        "versionNumber", v2.getVersionNumber(),
                        "sizeBytes", v2.getSizeBytes(),
                        "mimeType", v2.getMimeType(),
                        "checksumSha256", v2.getChecksumSha256() != null ? v2.getChecksumSha256() : "",
                        "uploadedBy", Map.of("id", v2.getUploadedBy().getUuid(), "name", v2.getUploadedBy().getFirstName() + " " + v2.getUploadedBy().getLastName()),
                        "createdAt", v2.getCreatedAt().toString(),
                        "downloadUrl", downloadUrl2
                ),
                "sizeDifference", Math.abs(v1.getSizeBytes() - v2.getSizeBytes()),
                "sameContent", sameContent
        );
    }

    @Transactional
    public void deleteAllVersions(FileEntity fileEntity) {
        var versions = fileVersionRepository.findByFileIdOrderByVersionNumberAsc(fileEntity.getId());
        for (FileVersion version : versions) {
            storageService.deleteObject(version.getStorageBucket(), version.getStorageKey());
        }
        fileVersionRepository.deleteAllByFileId(fileEntity.getId());
    }

    private String buildVersionStorageKey(FileEntity file, int versionNumber, String fileName) {
        return file.getOrganization().getId() + "/" +
                file.getWorkspace().getId() + "/" +
                file.getUuid() + "/versions/v" + versionNumber + "_" + fileName;
    }
}
