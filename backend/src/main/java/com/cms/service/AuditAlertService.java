package com.cms.service;

import com.cms.entity.*;
import com.cms.repository.AuditAlertInstanceRepository;
import com.cms.repository.AuditAlertRuleRepository;
import com.cms.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditAlertService {

    private final AuditAlertRuleRepository ruleRepository;
    private final AuditAlertInstanceRepository instanceRepository;
    private final AuditEventRepository auditEventRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String COUNTER_PREFIX = "audit:alert:counter:";

    public void evaluateThresholds(AuditEvent event) {
        Long orgId = event.getOrganization().getId();
        List<AuditAlertRule> rules = ruleRepository.findByOrganizationIdAndEnabledTrue(orgId);

        for (AuditAlertRule rule : rules) {
            if (!rule.getEventType().equals(event.getEventType())) continue;

            String counterKey = COUNTER_PREFIX + rule.getId() + ":" +
                    (event.getUser() != null ? event.getUser().getId() : "anon");
            try {
                Long count = redisTemplate.opsForValue().increment(counterKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(counterKey, Duration.ofMinutes(rule.getTimeWindowMinutes()));
                }
                if (count != null && count >= rule.getThresholdCount()) {
                    triggerAlert(rule, event, count.intValue());
                    redisTemplate.delete(counterKey);
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate alert rule {} for event {}", rule.getId(), event.getId(), e);
            }
        }
    }

    private void triggerAlert(AuditAlertRule rule, AuditEvent event, int eventCount) {
        Instant now = Instant.now();
        AuditAlertInstance instance = AuditAlertInstance.builder()
                .rule(rule)
                .organization(event.getOrganization())
                .triggeredByUser(event.getUser())
                .eventCount(eventCount)
                .windowStart(now.minus(Duration.ofMinutes(rule.getTimeWindowMinutes())))
                .windowEnd(now)
                .build();
        instanceRepository.save(instance);
        log.warn("Alert triggered: rule='{}', user='{}', count={}", rule.getName(),
                event.getUser() != null ? event.getUser().getEmail() : "anonymous", eventCount);
    }

    public void acknowledgeAlert(String uuid, Long orgId, User acknowledgedBy) {
        AuditAlertInstance instance = instanceRepository.findByUuidAndOrganizationId(uuid, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        instance.setAcknowledged(true);
        instance.setAcknowledgedBy(acknowledgedBy);
        instance.setAcknowledgedAt(Instant.now());
        instanceRepository.save(instance);
    }
}
