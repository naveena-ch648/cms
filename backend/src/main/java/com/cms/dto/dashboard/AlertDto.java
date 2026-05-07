package com.cms.dto.dashboard;

import com.cms.entity.UserAlert;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDto {

    private String id;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private String targetType;
    private String targetId;
    private Instant createdAt;

    public static AlertDto from(UserAlert alert) {
        return AlertDto.builder()
                .id(alert.getUuid())
                .alertType(alert.getAlertType().name())
                .severity(alert.getSeverity().name())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .targetType(alert.getTargetType())
                .targetId(alert.getTargetId())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
