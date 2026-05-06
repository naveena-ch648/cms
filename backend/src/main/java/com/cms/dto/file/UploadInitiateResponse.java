package com.cms.dto.file;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadInitiateResponse {

    private String sessionId;
    private Long chunkSize;
    private Integer totalChunks;
    private Instant expiresAt;
}
