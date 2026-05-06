package com.cms.dto.file;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkUploadResponse {

    private Integer chunkNumber;
    private boolean received;
    private Integer completedChunks;
    private Integer totalChunks;
}
