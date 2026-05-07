package com.cms.service;

import com.cms.dto.workflow.TriggerCreateRequest;
import com.cms.dto.workflow.TriggerResponse;
import com.cms.entity.*;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.FileRepository;
import com.cms.repository.WorkflowTriggerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerService {

    private final WorkflowTriggerRepository triggerRepository;
    private final FileRepository fileRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TriggerResponse create(Long workspaceId, TriggerCreateRequest request, User creator) {
        WorkflowTrigger.TriggerType type;
        try {
            type = WorkflowTrigger.TriggerType.valueOf(request.getTriggerType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_TRIGGER_TYPE",
                    "Invalid trigger type: " + request.getTriggerType() + ". Use NOTIFICATION or PREREQUISITE");
        }

        String configJson = null;
        if (request.getConfig() != null) {
            try {
                configJson = objectMapper.writeValueAsString(request.getConfig());
            } catch (Exception e) {
                throw new BusinessRuleException("INVALID_CONFIG", "Invalid trigger config JSON");
            }
        }

        WorkflowTrigger trigger = WorkflowTrigger.builder()
                .workspace(Workspace.builder().id(workspaceId).build())
                .name(request.getName())
                .triggerState(request.getTriggerState().toUpperCase())
                .triggerType(type)
                .config(configJson)
                .enabled(request.isEnabled())
                .createdBy(creator)
                .build();

        trigger = triggerRepository.save(trigger);
        log.info("Trigger created: id={} name={} workspace={}", trigger.getUuid(), trigger.getName(), workspaceId);
        return TriggerResponse.from(trigger);
    }

    @Transactional
    public TriggerResponse update(String triggerUuid, TriggerCreateRequest request) {
        WorkflowTrigger trigger = triggerRepository.findByUuid(triggerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Trigger not found: " + triggerUuid));

        if (request.getName() != null) {
            trigger.setName(request.getName());
        }
        if (request.getTriggerState() != null) {
            trigger.setTriggerState(request.getTriggerState().toUpperCase());
        }
        if (request.getTriggerType() != null) {
            try {
                trigger.setTriggerType(WorkflowTrigger.TriggerType.valueOf(request.getTriggerType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("INVALID_TRIGGER_TYPE", "Invalid trigger type: " + request.getTriggerType());
            }
        }
        if (request.getConfig() != null) {
            try {
                trigger.setConfig(objectMapper.writeValueAsString(request.getConfig()));
            } catch (Exception e) {
                throw new BusinessRuleException("INVALID_CONFIG", "Invalid trigger config JSON");
            }
        }

        trigger = triggerRepository.save(trigger);
        return TriggerResponse.from(trigger);
    }

    @Transactional
    public void delete(String triggerUuid) {
        WorkflowTrigger trigger = triggerRepository.findByUuid(triggerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Trigger not found: " + triggerUuid));
        triggerRepository.delete(trigger);
    }

    @Transactional
    public TriggerResponse toggle(String triggerUuid, boolean enabled) {
        WorkflowTrigger trigger = triggerRepository.findByUuid(triggerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Trigger not found: " + triggerUuid));
        trigger.setEnabled(enabled);
        trigger = triggerRepository.save(trigger);
        return TriggerResponse.from(trigger);
    }

    @Transactional(readOnly = true)
    public List<TriggerResponse> listByWorkspace(Long workspaceId) {
        return triggerRepository.findByWorkspaceId(workspaceId).stream()
                .map(TriggerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Execute prerequisite triggers for a given file and target state.
     * Returns null if all pass, or throws BusinessRuleException if blocked.
     */
    @Transactional(readOnly = true)
    public void executeTriggers(String fileUuid, String targetState) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        List<WorkflowTrigger> triggers = triggerRepository
                .findByWorkspaceIdAndTriggerStateAndEnabled(file.getWorkspace().getId(), targetState, true);

        for (WorkflowTrigger trigger : triggers) {
            if (trigger.getTriggerType() == WorkflowTrigger.TriggerType.PREREQUISITE) {
                evaluatePrerequisite(trigger, file);
            }
            // NOTIFICATION triggers are fire-and-forget (handled elsewhere)
        }
    }

    private void evaluatePrerequisite(WorkflowTrigger trigger, FileEntity file) {
        if (trigger.getConfig() == null || trigger.getConfig().isEmpty()) {
            return;
        }

        try {
            Map<String, Object> config = objectMapper.readValue(trigger.getConfig(), new com.fasterxml.jackson.core.type.TypeReference<>() {});

            // Check for required_metadata prerequisite
            if (config.containsKey("required_metadata")) {
                @SuppressWarnings("unchecked")
                List<String> requiredFields = (List<String>) config.get("required_metadata");
                // This would check if the file has the required metadata fields
                // For now, we check against file description as a simple implementation
                if (requiredFields != null && !requiredFields.isEmpty()) {
                    // TODO: Integrate with MetadataValueRepository to check actual metadata
                    log.debug("Prerequisite check: required_metadata={} for file={}", requiredFields, file.getUuid());
                }
            }

            // Check for required_tags prerequisite
            if (config.containsKey("required_tags")) {
                @SuppressWarnings("unchecked")
                List<String> requiredTags = (List<String>) config.get("required_tags");
                if (requiredTags != null && !requiredTags.isEmpty()) {
                    // TODO: Integrate with TagRepository to check actual tags
                    log.debug("Prerequisite check: required_tags={} for file={}", requiredTags, file.getUuid());
                }
            }

        } catch (Exception e) {
            log.warn("Failed to evaluate prerequisite trigger {}: {}", trigger.getUuid(), e.getMessage());
            throw new BusinessRuleException("TRIGGER_EVALUATION_FAILED",
                    "Prerequisite trigger '" + trigger.getName() + "' failed to evaluate");
        }
    }
}
