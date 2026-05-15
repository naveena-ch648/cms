import com.cms.entity.*;
import com.cms.repository.AuditAlertInstanceRepository;
import com.cms.repository.AuditAlertRuleRepository;
import com.cms.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditAlertService {

    private final AuditAlertRuleRepository ruleRepository;
    private final AuditAlertInstanceRepository instanceRepository;
    private final AuditEventRepository auditEventRepository;

    private static final String COUNTER_PREFIX = "audit:alert:counter:";
    private record Window(AtomicLong count, Instant expiresAt) {}
    private final ConcurrentHashMap<String, Window> counters = new ConcurrentHashMap<>();

    public void evaluateThresholds(AuditEvent event) {
        Long orgId = event.getOrganization().getId();
        List<AuditAlertRule> rules = ruleRepository.findByOrganizationIdAndEnabledTrue(orgId);

        for (AuditAlertRule rule : rules) {
            if (!rule.getEventType().equals(event.getEventType())) continue;

            String counterKey = COUNTER_PREFIX + rule.getId() + ":" +
                    (event.getUser() != null ? event.getUser().getId() : "anon");
            try {
                Instant now = Instant.now();
                Duration window = Duration.ofMinutes(rule.getTimeWindowMinutes());
                counters.compute(counterKey, (k, existing) -> {
                    if (existing == null || now.isAfter(existing.expiresAt())) {
                        return new Window(new AtomicLong(1), now.plus(window));
                    }
                    existing.count().incrementAndGet();
                    return existing;
                });
                Window w = counters.get(counterKey);
                if (w != null && w.count().get() >= rule.getThresholdCount()) {
                    triggerAlert(rule, event, (int) w.count().get());
                    counters.remove(counterKey);
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
