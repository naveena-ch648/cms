package com.cms.event;

import com.cms.service.JobQueueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileIndexEventPublisher {

    private static final String QUEUE_NAME = "search:index";

    private final JobQueueService jobQueueService;
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
            jobQueueService.push(QUEUE_NAME, event);
            log.debug("Published search index event: action={}, fileId={}", action, fileUuid);
        } catch (Exception e) {
            log.error("Failed to publish search index event for fileId={}: {}", fileUuid, e.getMessage());
        }
    }
}
