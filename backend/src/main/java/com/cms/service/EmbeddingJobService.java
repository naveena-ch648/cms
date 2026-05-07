package com.cms.service;

import com.cms.config.LLMConfig;
import com.cms.entity.EmbeddingJob;
import com.cms.entity.EmbeddingJob.JobStatus;
import com.cms.entity.FileEntity;
import com.cms.repository.EmbeddingJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class EmbeddingJobService {

    private static final String EMBEDDING_QUEUE = "embedding:process";

    private final EmbeddingJobRepository embeddingJobRepository;
    private final StringRedisTemplate redisTemplate;
    private final LLMConfig llmConfig;
    private final ObjectMapper objectMapper;

    public EmbeddingJobService(EmbeddingJobRepository embeddingJobRepository,
                               StringRedisTemplate redisTemplate,
                               LLMConfig llmConfig,
                               ObjectMapper objectMapper) {
        this.embeddingJobRepository = embeddingJobRepository;
        this.redisTemplate = redisTemplate;
        this.llmConfig = llmConfig;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EmbeddingJob dispatchEmbeddingJob(FileEntity file) {
        // Check if there's already a pending/processing job
        boolean exists = embeddingJobRepository.existsByFileIdAndStatusIn(
                file.getId(), List.of(JobStatus.PENDING, JobStatus.PROCESSING));
        if (exists) {
            return embeddingJobRepository.findTopByFileIdOrderByCreatedAtDesc(file.getId()).orElse(null);
        }

        // Create embedding job record
        EmbeddingJob job = EmbeddingJob.builder()
                .uuid(UUID.randomUUID().toString())
                .file(file)
                .organization(file.getOrganization())
                .workspace(file.getWorkspace())
                .embeddingModel(llmConfig.getEmbeddingModel())
                .vectorDimension(llmConfig.getEmbeddingDimension())
                .build();

        job = embeddingJobRepository.save(job);

        // Push job to Redis queue
        try {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("fileId", file.getId());
            jobData.put("fileUuid", file.getUuid());
            jobData.put("organizationId", file.getOrganization().getId());
            jobData.put("workspaceId", file.getWorkspace().getId());
            jobData.put("storageBucket", file.getStorageBucket());
            jobData.put("storageKey", file.getStorageKey());
            jobData.put("mimeType", file.getMimeType());
            jobData.put("fileName", file.getName());
            jobData.put("jobUuid", job.getUuid());

            String payload = objectMapper.writeValueAsString(jobData);
            redisTemplate.opsForList().leftPush(EMBEDDING_QUEUE, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to dispatch embedding job", e);
        }

        return job;
    }

    public Optional<EmbeddingJob> getLatestJob(Long fileId) {
        return embeddingJobRepository.findTopByFileIdOrderByCreatedAtDesc(fileId);
    }

    @Transactional
    public EmbeddingJob retriggerEmbedding(FileEntity file) {
        // Delete old job if failed
        embeddingJobRepository.findTopByFileIdOrderByCreatedAtDesc(file.getId())
                .ifPresent(existingJob -> {
                    if (existingJob.getStatus() == JobStatus.FAILED || existingJob.getStatus() == JobStatus.COMPLETED) {
                        // Allow re-trigger
                    } else {
                        throw new IllegalStateException("Document is already being processed");
                    }
                });

        // Create new job
        return dispatchEmbeddingJob(file);
    }
}
