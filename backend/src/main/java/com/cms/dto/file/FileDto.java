package com.cms.dto.file;

import com.cms.entity.FileEntity;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDto {

    private String id;
    private String name;
    private String originalName;
    private Long sizeBytes;
    private String mimeType;
    private String folderId;
    private String folderName;
    private String workspaceId;
    private String status;
    private String checksumSha256;
    private String description;
    private List<String> tags;
    private Integer downloadCount;
    private Instant lastAccessedAt;
    private UploadedByDto uploadedBy;
    private Instant uploadCompletedAt;
    private String thumbnailUrl;
    private boolean previewable;
    private Instant trashedAt;
    private Instant permanentDeleteAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadedByDto {
        private String id;
        private String name;
    }

    public static FileDto from(FileEntity file) {
        return FileDto.builder()
                .id(file.getUuid())
                .name(file.getName())
                .originalName(file.getOriginalName())
                .sizeBytes(file.getSizeBytes())
                .mimeType(file.getMimeType())
                .folderId(file.getFolder() != null ? file.getFolder().getUuid() : null)
                .folderName(file.getFolder() != null ? file.getFolder().getName() : null)
                .workspaceId(file.getWorkspace() != null ? file.getWorkspace().getUuid() : null)
                .status(file.getStatus().name())
                .checksumSha256(file.getChecksumSha256())
                .description(file.getDescription())
                .downloadCount(file.getDownloadCount())
                .lastAccessedAt(file.getLastAccessedAt())
                .uploadedBy(file.getUploadedBy() != null ? UploadedByDto.builder()
                        .id(file.getUploadedBy().getUuid())
                        .name(file.getUploadedBy().getFirstName() + " " + file.getUploadedBy().getLastName())
                        .build() : null)
                .uploadCompletedAt(file.getUploadCompletedAt())
                .trashedAt(file.getTrashedAt())
                .permanentDeleteAt(file.getPermanentDeleteAt())
                .previewable(isPreviewable(file.getMimeType()))
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    private static boolean isPreviewable(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.startsWith("image/") ||
               mimeType.equals("application/pdf") ||
               mimeType.startsWith("text/") ||
               mimeType.startsWith("video/") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
               mimeType.equals("application/msword") ||
               mimeType.equals("application/vnd.ms-excel") ||
               mimeType.equals("application/vnd.ms-powerpoint");
    }
}
