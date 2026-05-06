package com.cms.scheduler;

import com.cms.entity.FileEntity;
import com.cms.entity.FileEntity.FileStatus;
import com.cms.repository.FileRepository;
import com.cms.service.StorageQuotaService;
import com.cms.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrashCleanupJob {

    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final StorageQuotaService storageQuotaService;

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void cleanupExpiredTrash() {
        List<FileEntity> expired = fileRepository.findByStatusAndPermanentDeleteAtBefore(
                FileStatus.TRASHED, Instant.now());

        if (expired.isEmpty()) return;

        int deleted = 0;
        for (FileEntity file : expired) {
            try {
                storageService.deleteObject(file.getStorageBucket(), file.getStorageKey());
                if (file.getThumbnailKey() != null) {
                    storageService.deleteObject(file.getStorageBucket(), file.getThumbnailKey());
                }
                storageQuotaService.updateUsedStorage(file.getOrganization().getId(), -file.getSizeBytes());
                fileRepository.delete(file);
                deleted++;
            } catch (Exception e) {
                log.warn("Failed to permanently delete file {}: {}", file.getUuid(), e.getMessage());
            }
        }

        log.info("Permanently deleted {} expired trash files", deleted);
    }
}
