package com.cms.service;

import com.cms.dto.dashboard.ActivityEventDto;
import com.cms.entity.ActivityEvent;
import com.cms.entity.Organization;
import com.cms.entity.User;
import com.cms.entity.Workspace;
import com.cms.repository.ActivityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityEventService {

    private static final String DASHBOARD_SUMMARY_KEY_PREFIX = "dashboard:summary:";

    private final ActivityEventRepository activityEventRepository;

    @Async
    @Transactional
    public void recordEvent(User actor, ActivityEvent.ActionType actionType,
                            String targetType, String targetId, String targetName,
                            Workspace workspace, Organization organization, String metadata) {
        try {
            ActivityEvent event = ActivityEvent.builder()
                    .actor(actor)
                    .actorName(actor.getFirstName() + " " + actor.getLastName())
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .targetName(targetName)
                    .workspace(workspace)
                    .organization(organization)
                    .metadata(metadata)
                    .build();
            activityEventRepository.save(event);
            // Dashboard cache invalidation removed (no Redis)
        } catch (Exception e) {
            log.error("Failed to record activity event: {} for target {}", actionType, targetId, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<ActivityEventDto> getActivityFeed(List<Long> workspaceIds, Pageable pageable) {
        return activityEventRepository.findByWorkspaceIdInOrderByCreatedAtDesc(workspaceIds, pageable)
                .map(ActivityEventDto::from);
    }

    @Transactional(readOnly = true)
    public Page<ActivityEventDto> getActivityByActor(Long actorId, Pageable pageable) {
        return activityEventRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable)
                .map(ActivityEventDto::from);
    }
}
