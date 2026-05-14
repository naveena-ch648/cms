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
    private int memberCount;
    private RoleSummary myRole;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleSummary {
        private String id;
        private String name;
        private String source;
    }

    public static WorkspaceResponse from(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getUuid())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .status(workspace.getStatus().name())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .memberCount(0)
                .build();
    }

    public static WorkspaceResponse from(Workspace workspace, int memberCount, com.cms.entity.Role myRole) {
        RoleSummary roleSummary = myRole != null
                ? RoleSummary.builder()
                        .id(myRole.getUuid())
                        .name(myRole.getName())
                        .source("direct")
                        .build()
                : null;
        return WorkspaceResponse.builder()
                .id(workspace.getUuid())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .status(workspace.getStatus().name())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .memberCount(memberCount)
                .myRole(roleSummary)
                .build();
    }
}
