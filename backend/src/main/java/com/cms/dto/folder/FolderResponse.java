package com.cms.dto.folder;

import com.cms.entity.Folder;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResponse {

    private String id;
    private String name;
    private String parentId;
    private String workspaceId;
    private Integer sortOrder;
    private String status;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BreadcrumbItem> breadcrumbs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BreadcrumbItem {
        private String id;
        private String name;
    }

    public static FolderResponse from(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getUuid())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getUuid() : null)
                .workspaceId(folder.getWorkspace().getUuid())
                .sortOrder(folder.getSortOrder())
                .status(folder.getStatus().name())
                .createdBy(folder.getCreatedBy() != null ? folder.getCreatedBy().getUuid() : null)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    public static FolderResponse from(Folder folder, List<BreadcrumbItem> breadcrumbs) {
        FolderResponse response = from(folder);
        response.setBreadcrumbs(breadcrumbs);
        return response;
    }
}
