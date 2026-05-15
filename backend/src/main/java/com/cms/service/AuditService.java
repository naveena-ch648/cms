package com.cms.service;

import com.cms.entity.*;
import com.cms.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final AuditSearchService auditSearchService;
    private final AuditAlertService auditAlertService;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

    private static final String BUFFER_QUEUE = "audit:buffer";

    @Async
    public void logAsync(Organization org, User user, AuditEventType eventType, AuditCategory category,
                         String resourceType, Long resourceId, String resourceName,
                         String outcome, String details, String ipAddress, String userAgent,
                         Workspace workspace) {
        AuditEvent event = AuditEvent.builder()
                .organization(org)
                .user(user)
                .eventType(eventType.name())
                .category(category)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .outcome(outcome != null ? outcome : "SUCCESS")
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .actorName(user != null ? user.getEmail() : "anonymous")
                .workspace(workspace)
                .build();

        AuditEvent saved = auditEventRepository.save(event);
        indexToOpenSearch(saved);
        evaluateAlerts(saved);
    }

    public void log(Organization org, User user, String eventType, String resourceType,
                    Long resourceId, String details, String ipAddress) {
        AuditEvent event = AuditEvent.builder()
                .organization(org)
                .user(user)
                .eventType(eventType)
                .category(AuditCategory.SYSTEM)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .outcome("SUCCESS")
                .details(toJsonDetails(details))
                .ipAddress(ipAddress)
                .actorName(user != null ? user.getEmail() : "system")
                .build();
        AuditEvent saved = auditEventRepository.save(event);
        indexToOpenSearch(saved);
    }

    /**
     * Ensures the details string is stored as valid JSON in the MySQL JSON column.
     * Plain strings (e.g. "Created folder: X") are wrapped as JSON strings.
     * Null is passed through as-is (SQL NULL is valid for JSON columns).
     */
    private String toJsonDetails(String details) {
        if (details == null) {
            return null;
        }
        try {
            // Already valid JSON (object, array, or quoted string) — use as-is
            objectMapper.readTree(details);
            return details;
        } catch (Exception e) {
            // Plain string — serialize it as a JSON string value
            try {
                return objectMapper.writeValueAsString(details);
            } catch (Exception ex) {
                log.warn("Could not JSON-encode audit details: {}", details);
                return null;
            }
        }
    }

    public void log(Organization org, User user, String eventType) {
        log(org, user, eventType, null, null, null, null);
    }

    public void log(Organization org, User user, String eventType, String ipAddress) {
        log(org, user, eventType, null, null, null, ipAddress);
    }

    private void indexToOpenSearch(AuditEvent event) {
        try {
            auditSearchService.indexEvent(event);
        } catch (Exception e) {
            log.warn("Failed to index audit event {} to OpenSearch, buffering in Redis", event.getId(), e);
            bufferEvent(event.getId());
        }
    }

    private void evaluateAlerts(AuditEvent event) {
        try {
            auditAlertService.evaluateThresholds(event);
        } catch (Exception e) {
            log.warn("Failed to evaluate alert thresholds for event {}", event.getId(), e);
        }
    }

    private void bufferEvent(Long eventId) {
        try {
            jobQueueService.push(BUFFER_QUEUE, Map.of("eventId", eventId));
        } catch (Exception e) {
            log.error("Failed to buffer audit event {} in job_queue", eventId, e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void retryBufferedEvents() {
        // Buffered events are retried by the worker polling job_queue
        // No direct Redis pop needed here — kept as no-op placeholder
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoff = Instant.now().minus(365, ChronoUnit.DAYS);
        int deleted = auditEventRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} audit events older than 365 days", deleted);
            try {
                auditSearchService.deleteEventsBefore(cutoff);
            } catch (Exception e) {
                log.warn("Failed to clean up old events from OpenSearch", e);
            }
        }
    }
}
