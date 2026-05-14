package com.cms.dto.dashboard;

import com.cms.entity.SharedLink;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedItemDto {

    private String id;
    private String fileName;
    private String fileId;
    private String workspaceName;
    private String sharedBy;
    private String sharedWith;
    private Instant sharedAt;
    private Instant expiresAt;
    private String type;

    public static SharedItemDto fromSharedByMe(SharedLink link) {
        String name = link.getFile() != null ? link.getFile().getName() : null;
        String resourceId = link.getFile() != null ? link.getFile().getUuid() : null;
        String workspaceName = link.getWorkspace() != null ? link.getWorkspace().getName() : null;
        return SharedItemDto.builder()
                .id(link.getUuid())
                .fileName(name)
                .fileId(resourceId)
                .workspaceName(workspaceName)
                .sharedBy(link.getCreatedBy() != null
                        ? link.getCreatedBy().getFirstName() + " " + link.getCreatedBy().getLastName()
                        : null)
                .sharedWith("via link")
                .sharedAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .type("SHARED_BY_ME")
                .build();
    }

    public static SharedItemDto fromSharedWithMe(SharedLink link) {
        String name = link.getFile() != null ? link.getFile().getName() : null;
        String resourceId = link.getFile() != null ? link.getFile().getUuid() : null;
        String workspaceName = link.getWorkspace() != null ? link.getWorkspace().getName() : null;
        String creatorName = link.getCreatedBy() != null
                ? link.getCreatedBy().getFirstName() + " " + link.getCreatedBy().getLastName()
                : "Someone";
        return SharedItemDto.builder()
                .id(link.getUuid())
                .fileName(name)
                .fileId(resourceId)
                .workspaceName(workspaceName)
                .sharedBy(creatorName)
                .sharedWith(null)
                .sharedAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .type("SHARED_WITH_ME")
                .build();
    }
}
