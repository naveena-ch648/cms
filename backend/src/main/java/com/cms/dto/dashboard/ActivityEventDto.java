package com.cms.dto.dashboard;

import com.cms.entity.ActivityEvent;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEventDto {

    private String id;
    private String actorName;
    private String actionType;
    private String targetType;
    private String targetId;
    private String targetName;
    private String workspaceName;
    private String metadata;
    private Instant createdAt;

    public static ActivityEventDto from(ActivityEvent event) {
        return ActivityEventDto.builder()
                .id(event.getUuid())
                .actorName(event.getActorName())
                .actionType(event.getActionType().name())
                .targetType(event.getTargetType())
                .targetId(event.getTargetId())
                .targetName(event.getTargetName())
                .workspaceName(event.getWorkspace() != null ? event.getWorkspace().getName() : null)
                .metadata(event.getMetadata())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
