package com.cms.service;

import com.cms.entity.FileEntity;
import com.cms.entity.FileVersion;
import com.cms.entity.PreviewJob;
import com.cms.entity.PreviewJob.JobStatus;
import com.cms.entity.PreviewJob.JobType;
import com.cms.repository.PreviewJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreviewJobDispatcher {

    private static final String QUEUE_NAME = "file:process";

    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;
    private final PreviewJobRepository previewJobRepository;

    @Transactional
    public PreviewJob dispatchPreviewJob(FileEntity file, FileVersion version, JobType jobType) {
        // Check for existing queued/processing job to avoid duplicates
        var existing = previewJobRepository.findByFileIdAndJobTypeAndStatus(file.getId(), jobType, JobStatus.QUEUED);
        if (existing.isPresent()) {
            log.debug("Preview job already queued for fileId={}, type={}", file.getId(), jobType);
            return existing.get();
        }

        PreviewJob job = PreviewJob.builder()
                .uuid(UUID.randomUUID().toString())
                .file(file)
                .version(version)
                .jobType(jobType)
                .status(JobStatus.QUEUED)
                .build();
        job = previewJobRepository.save(job);

        publishToQueue(file, version, jobType);
        log.info("Dispatched preview job: fileId={}, jobType={}, jobId={}", file.getUuid(), jobType, job.getUuid());
        return job;
    }

    private void publishToQueue(FileEntity file, FileVersion version, JobType jobType) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("fileId", file.getUuid());
            message.put("organizationId", file.getOrganization().getUuid());
            message.put("action", jobType == JobType.THUMBNAIL ? "thumbnail" : "preview");
            message.put("mimeType", file.getMimeType());
            message.put("storageBucket", file.getStorageBucket());
            message.put("storageKey", file.getStorageKey());
            if (version != null) {
                message.put("versionId", version.getUuid());
            }
            message.put("priority", 0);
            message.put("_retries", 0);

            String payload = objectMapper.writeValueAsString(message);
            jobQueueService.push(QUEUE_NAME, payload);
        } catch (Exception e) {
            log.error("Failed to publish preview job to queue for fileId={}", file.getUuid(), e);
        }
    }
}
