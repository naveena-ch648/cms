package com.cms.dto.collaboration;

import com.cms.entity.Notification;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {

    private String id;
    private String type;
    private String title;
    private String message;
    private String targetType;
    private String targetId;
    private ActorDto actor;
    private boolean read;
    private Instant readAt;
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

    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getUuid())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .actor(notification.getActor() != null ? ActorDto.builder()
                        .id(notification.getActor().getUuid())
                        .name(notification.getActor().getFirstName() + " " + notification.getActor().getLastName())
                        .build() : null)
                .read(Boolean.TRUE.equals(notification.getIsRead()))
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
