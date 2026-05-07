package com.cms.dto.workflow;

import com.cms.entity.WorkflowTrigger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggerResponse {

    private String id;
    private String name;
    private String triggerState;
    private String triggerType;
    private Map<String, Object> config;
    private boolean enabled;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static TriggerResponse from(WorkflowTrigger trigger) {
        Map<String, Object> configMap = null;
        if (trigger.getConfig() != null && !trigger.getConfig().isEmpty()) {
            try {
                configMap = objectMapper.readValue(trigger.getConfig(), new TypeReference<>() {});
            } catch (Exception e) {
                configMap = Map.of("raw", trigger.getConfig());
            }
        }

        return TriggerResponse.builder()
                .id(trigger.getUuid())
                .name(trigger.getName())
                .triggerState(trigger.getTriggerState())
                .triggerType(trigger.getTriggerType().name())
                .config(configMap)
                .enabled(trigger.getEnabled())
                .createdBy(trigger.getCreatedBy().getUuid())
                .createdAt(trigger.getCreatedAt())
                .updatedAt(trigger.getUpdatedAt())
                .build();
    }
}
