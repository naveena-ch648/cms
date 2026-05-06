package com.cms.dto.collaboration;

import com.cms.entity.AuditEvent;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEventDto {

    private Long id;
    private String eventType;
    private String category;
    private ActorDto actor;
    private String description;
    private String details;
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActorDto {
        private String id;
        private String name;
    }

    public static ActivityEventDto from(AuditEvent event) {
        return ActivityEventDto.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .category(mapCategory(event.getEventType()))
                .actor(event.getUser() != null ? ActorDto.builder()
                        .id(event.getUser().getUuid())
                        .name(event.getUser().getFirstName() + " " + event.getUser().getLastName())
                        .build() : null)
                .description(buildDescription(event))
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private static String mapCategory(String eventType) {
        if (eventType == null) return "OTHER";
        if (eventType.startsWith("COMMENT")) return "COMMENT";
        if (eventType.startsWith("TASK")) return "TASK";
        if (eventType.startsWith("FILE_UPLOAD") || eventType.equals("FILE_UPLOADED")) return "UPLOAD";
        if (eventType.startsWith("FILE_VERSION")) return "VERSION";
        if (eventType.contains("PERMISSION") || eventType.contains("SHARE") || eventType.contains("SHARED_LINK")) return "SHARE";
        return "OTHER";
    }

    private static String buildDescription(AuditEvent event) {
        String actorName = event.getUser() != null
                ? event.getUser().getFirstName() + " " + event.getUser().getLastName()
                : "System";
        return switch (event.getEventType()) {
            case "COMMENT_CREATED" -> actorName + " posted a comment";
            case "COMMENT_DELETED" -> actorName + " deleted a comment";
            case "TASK_CREATED" -> actorName + " created a task";
            case "TASK_COMPLETED" -> actorName + " completed a task";
            case "TASK_REOPENED" -> actorName + " reopened a task";
            case "FILE_UPLOADED" -> actorName + " uploaded the file";
            case "FILE_VERSION_CREATED" -> actorName + " uploaded a new version";
            default -> actorName + " performed " + event.getEventType();
        };
    }
}
