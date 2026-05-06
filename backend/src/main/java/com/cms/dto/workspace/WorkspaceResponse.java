package com.cms.dto.workspace;

import com.cms.entity.Workspace;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceResponse {

    private String id;
    private String name;
    private String description;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public static WorkspaceResponse from(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getUuid())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .status(workspace.getStatus().name())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }
}
