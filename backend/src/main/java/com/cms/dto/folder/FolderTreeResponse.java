package com.cms.dto.folder;

import com.cms.entity.Folder;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderTreeResponse {

    private String id;
    private String name;
    private String parentId;
    private Integer sortOrder;
    private String status;
    private Long childCount;
    private Instant createdAt;

    public static FolderTreeResponse from(Folder folder, long childCount) {
        return FolderTreeResponse.builder()
                .id(folder.getUuid())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getUuid() : null)
                .sortOrder(folder.getSortOrder())
                .status(folder.getStatus().name())
                .childCount(childCount)
                .createdAt(folder.getCreatedAt())
                .build();
    }
}
