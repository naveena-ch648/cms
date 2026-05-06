package com.cms.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileIndexEventPublisher {

    private static final String QUEUE_NAME = "search:index";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishIndexEvent(String fileUuid, String workspaceUuid, String organizationUuid) {
        publishEvent("index", fileUuid, workspaceUuid, organizationUuid);
    }

    public void publishDeleteEvent(String fileUuid, String workspaceUuid, String organizationUuid) {
        publishEvent("delete", fileUuid, workspaceUuid, organizationUuid);
    }

    private void publishEvent(String action, String fileUuid, String workspaceUuid, String organizationUuid) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("action", action);
            event.put("fileId", fileUuid);
            event.put("workspaceId", workspaceUuid);
            event.put("organizationId", organizationUuid);
            event.put("timestamp", Instant.now().toString());

            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.opsForList().leftPush(QUEUE_NAME, payload);
            log.debug("Published search index event: action={}, fileId={}", action, fileUuid);
        } catch (Exception e) {
            log.error("Failed to publish search index event for fileId={}: {}", fileUuid, e.getMessage());
        }
    }
}
