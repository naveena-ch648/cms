package com.cms.dto.file;

import com.cms.entity.FileVersion;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersionDto {

    private String id;
    private Integer versionNumber;
    private String fileName;
    private Long sizeBytes;
    private String mimeType;
    private String checksumSha256;
    private String changeNote;
    private UploadedByDto uploadedBy;
    private Instant createdAt;
    private boolean isCurrent;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadedByDto {
        private String id;
        private String name;
    }

    public static FileVersionDto from(FileVersion version, boolean isCurrent) {
        return FileVersionDto.builder()
                .id(version.getUuid())
                .versionNumber(version.getVersionNumber())
                .fileName(version.getFile().getName())
                .sizeBytes(version.getSizeBytes())
                .mimeType(version.getMimeType())
                .checksumSha256(version.getChecksumSha256())
                .changeNote(version.getChangeNote())
                .uploadedBy(UploadedByDto.builder()
                        .id(version.getUploadedBy().getUuid())
                        .name(version.getUploadedBy().getFirstName() + " " + version.getUploadedBy().getLastName())
                        .build())
                .createdAt(version.getCreatedAt())
                .isCurrent(isCurrent)
                .build();
    }
}
