package com.cms.dto.file;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadSessionStatusDto {

    private String sessionId;
    private String fileName;
    private Integer totalChunks;
    private Integer completedChunks;
    private Double percentComplete;
    private String status;
    private Instant expiresAt;
    private Instant lastActivityAt;
}
