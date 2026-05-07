package com.cms.dto.dashboard;

import com.cms.entity.FileEntity;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentFileDto {

    private String id;
    private String name;
    private String mimeType;
    private long sizeBytes;
    private String workspaceId;
    private String workspaceName;
    private String folderId;
    private String folderPath;
    private Instant lastAccessedAt;
    private Instant updatedAt;

    public static RecentFileDto from(FileEntity file) {
        return RecentFileDto.builder()
                .id(file.getUuid())
                .name(file.getName())
                .mimeType(file.getMimeType())
                .sizeBytes(file.getSizeBytes() != null ? file.getSizeBytes() : 0)
                .workspaceId(file.getWorkspace() != null ? file.getWorkspace().getUuid() : null)
                .workspaceName(file.getWorkspace() != null ? file.getWorkspace().getName() : null)
                .folderId(file.getFolder() != null ? file.getFolder().getUuid() : null)
                .folderPath(file.getFolder() != null ? file.getFolder().getName() : "/")
                .lastAccessedAt(file.getLastAccessedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
