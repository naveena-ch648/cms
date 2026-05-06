package com.cms.service;

import com.cms.dto.preview.PreviewDto;
import com.cms.dto.preview.ThumbnailDto;
import com.cms.entity.FileEntity;
import com.cms.entity.FileVersion;
import com.cms.entity.Preview;
import com.cms.entity.Preview.PreviewStatus;
import com.cms.entity.Preview.PreviewType;
import com.cms.entity.PreviewJob;
import com.cms.entity.PreviewJob.JobType;
import com.cms.repository.FileRepository;
import com.cms.repository.PreviewJobRepository;
import com.cms.repository.PreviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreviewService {

    private static final Duration PRESIGN_EXPIRY = Duration.ofHours(1);
    private static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024; // 100MB

    private final PreviewRepository previewRepository;
    private final PreviewJobRepository previewJobRepository;
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final PreviewJobDispatcher previewJobDispatcher;

    @Transactional(readOnly = true)
    public PreviewDto getPreview(FileEntity file) {
        Optional<Preview> preview = previewRepository.findByFileIdAndTypeAndStatus(
                file.getId(), PreviewType.FULL_PREVIEW, PreviewStatus.COMPLETED);

        if (preview.isPresent()) {
            return buildPreviewDto(preview.get(), file);
        }

        // Check if there's a pending/processing preview
        List<Preview> pending = previewRepository.findByFileIdAndType(file.getId(), PreviewType.FULL_PREVIEW);
        for (Preview p : pending) {
            if (p.getStatus() == PreviewStatus.PENDING || p.getStatus() == PreviewStatus.PROCESSING) {
                return PreviewDto.from(p);
            }
        }

        return null;
    }

    @Transactional
    public PreviewDto getOrTriggerPreview(FileEntity file) {
        if (file.getSizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum preview size of 100MB");
        }

        PreviewDto existing = getPreview(file);
        if (existing != null) {
            return existing;
        }

        // Trigger preview generation
        previewJobDispatcher.dispatchPreviewJob(file, null, JobType.FULL_PREVIEW);

        // Create pending preview record
        Preview preview = Preview.builder()
                .uuid(UUID.randomUUID().toString())
                .file(file)
                .type(PreviewType.FULL_PREVIEW)
                .status(PreviewStatus.PENDING)
                .storageBucket(file.getStorageBucket())
                .build();
        preview = previewRepository.save(preview);

        return PreviewDto.from(preview);
    }

    @Transactional(readOnly = true)
    public ThumbnailDto getThumbnail(FileEntity file) {
        Optional<Preview> thumbnail = previewRepository.findByFileIdAndTypeAndStatus(
                file.getId(), PreviewType.THUMBNAIL, PreviewStatus.COMPLETED);

        if (thumbnail.isEmpty()) {
            return null;
        }

        Preview t = thumbnail.get();
        String url = storageService.presignGetUrl(t.getStorageBucket(), t.getThumbnailKey(), PRESIGN_EXPIRY);

        return ThumbnailDto.builder()
                .url(url)
                .width(t.getWidth() != null ? t.getWidth() : 256)
                .height(t.getHeight() != null ? t.getHeight() : 256)
                .expiresAt(Instant.now().plus(PRESIGN_EXPIRY))
                .build();
    }

    @Transactional
    public PreviewJob regeneratePreview(FileEntity file) {
        // Invalidate existing previews
        List<Preview> existing = previewRepository.findByFileId(file.getId());
        for (Preview p : existing) {
            if (p.getStatus() == PreviewStatus.COMPLETED || p.getStatus() == PreviewStatus.FAILED) {
                p.setStatus(PreviewStatus.PENDING);
                previewRepository.save(p);
            }
        }

        return previewJobDispatcher.dispatchPreviewJob(file, null, JobType.FULL_PREVIEW);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPreviewStatus(FileEntity file) {
        Map<String, Object> status = new HashMap<>();

        // Thumbnail status
        Optional<PreviewJob> thumbnailJob = previewJobRepository
                .findTopByFileIdAndJobTypeOrderByQueuedAtDesc(file.getId(), JobType.THUMBNAIL);
        if (thumbnailJob.isPresent()) {
            PreviewJob job = thumbnailJob.get();
            Map<String, Object> thumbStatus = new HashMap<>();
            thumbStatus.put("status", job.getStatus().name());
            if (job.getCompletedAt() != null) {
                thumbStatus.put("generatedAt", job.getCompletedAt().toString());
            }
            status.put("thumbnail", thumbStatus);
        }

        // Full preview status
        Optional<PreviewJob> previewJob = previewJobRepository
                .findTopByFileIdAndJobTypeOrderByQueuedAtDesc(file.getId(), JobType.FULL_PREVIEW);
        if (previewJob.isPresent()) {
            PreviewJob job = previewJob.get();
            Map<String, Object> previewStatus = new HashMap<>();
            previewStatus.put("status", job.getStatus().name());
            previewStatus.put("attempts", job.getAttempts());
            previewStatus.put("queuedAt", job.getQueuedAt().toString());
            status.put("fullPreview", previewStatus);
        }

        return status;
    }

    @Transactional
    public void dispatchThumbnailJob(FileEntity file) {
        if (file.getSizeBytes() > MAX_FILE_SIZE_BYTES) {
            log.debug("File {} exceeds size limit, skipping thumbnail generation", file.getUuid());
            return;
        }
        previewJobDispatcher.dispatchPreviewJob(file, null, JobType.THUMBNAIL);
    }

    private PreviewDto buildPreviewDto(Preview preview, FileEntity file) {
        PreviewDto dto = PreviewDto.from(preview);

        String mimeType = file.getMimeType();
        if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
            // Direct URL for images and videos
            String url = storageService.presignGetUrl(
                    file.getStorageBucket(), file.getStorageKey(), PRESIGN_EXPIRY);
            dto.setDirectUrl(url);
            dto.setExpiresAt(Instant.now().plus(PRESIGN_EXPIRY));
        } else if (preview.getPageCount() != null && preview.getPageCount() > 0) {
            // Page-based preview for PDF/Office
            List<PreviewDto.PageDto> pages = IntStream.rangeClosed(1, preview.getPageCount())
                    .mapToObj(i -> {
                        String pageKey = preview.getStorageKeyPrefix() + "/page-" + i + ".png";
                        String url = storageService.presignGetUrl(
                                preview.getStorageBucket(), pageKey, PRESIGN_EXPIRY);
                        return PreviewDto.PageDto.builder()
                                .page(i)
                                .url(url)
                                .width(preview.getWidth() != null ? preview.getWidth() : 794)
                                .height(preview.getHeight() != null ? preview.getHeight() : 1123)
                                .build();
                    })
                    .toList();
            dto.setPages(pages);
            dto.setExpiresAt(Instant.now().plus(PRESIGN_EXPIRY));
        }

        return dto;
    }
}
