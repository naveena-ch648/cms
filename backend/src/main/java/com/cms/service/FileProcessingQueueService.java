package com.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessingQueueService {

    private static final String QUEUE_NAME = "file:process";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishJob(Long fileId, Long organizationId, String action) {
        try {
            Map<String, Object> job = new HashMap<>();
            job.put("fileId", fileId);
            job.put("organizationId", organizationId);
            job.put("action", action);
            job.put("timestamp", System.currentTimeMillis());

            String payload = objectMapper.writeValueAsString(job);
            redisTemplate.opsForList().leftPush(QUEUE_NAME, payload);
            log.debug("Published file processing job: fileId={}, action={}", fileId, action);
        } catch (Exception e) {
            log.error("Failed to publish file processing job for fileId={}", fileId, e);
        }
    }
}
