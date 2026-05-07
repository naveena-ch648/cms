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
    private String sharedBy;
    private String sharedWith;
    private Instant sharedAt;
    private Instant expiresAt;
    private String type;

    public static SharedItemDto fromSharedByMe(SharedLink link) {
        String name = null;
        String resourceId = null;
        if (link.getResourceType() == SharedLink.ResourceType.FILE && link.getFile() != null) {
            name = link.getFile().getName();
            resourceId = link.getFile().getUuid();
        } else if (link.getFile() != null) {
            name = link.getFile().getName();
            resourceId = link.getFile().getUuid();
        }
        return SharedItemDto.builder()
                .id(link.getUuid())
                .fileName(name)
                .fileId(resourceId)
                .sharedBy(link.getCreatedBy() != null ? link.getCreatedBy().getFirstName() + " " + link.getCreatedBy().getLastName() : null)
                .sharedWith(null)
                .sharedAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .type("SHARED_BY_ME")
                .build();
    }
}
