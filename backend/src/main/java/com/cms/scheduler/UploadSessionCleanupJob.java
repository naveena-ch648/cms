package com.cms.scheduler;

import com.cms.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadSessionCleanupJob {

    private final RedisTemplate<String, String> redisTemplate;
    private final StorageService storageService;

    private static final String SESSION_PREFIX = "upload_session:";
    private static final Duration SESSION_MAX_AGE = Duration.ofHours(24);

    @Scheduled(fixedRate = 3600000) // every hour
    public void cleanupExpiredSessions() {
        Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return;

        int cleaned = 0;
        for (String key : keys) {
            try {
                Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
                if (session.isEmpty()) continue;

                String createdAtStr = (String) session.get("createdAt");
                if (createdAtStr == null) continue;

                Instant createdAt = Instant.parse(createdAtStr);
                if (Instant.now().isAfter(createdAt.plus(SESSION_MAX_AGE))) {
                    // Abort S3 multipart upload
                    String bucket = (String) session.get("s3Bucket");
                    String storageKey = (String) session.get("s3Key");
                    String uploadId = (String) session.get("s3UploadId");

                    if (bucket != null && storageKey != null && uploadId != null) {
                        try {
                            storageService.abortMultipartUpload(bucket, storageKey, uploadId);
                        } catch (Exception e) {
                            log.warn("Failed to abort multipart upload for session {}: {}", key, e.getMessage());
                        }
                    }

                    redisTemplate.delete(key);
                    cleaned++;
                }
            } catch (Exception e) {
                log.warn("Error cleaning up session {}: {}", key, e.getMessage());
            }
        }

        if (cleaned > 0) {
            log.info("Cleaned up {} expired upload sessions", cleaned);
        }
    }
}
