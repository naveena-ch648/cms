package com.cms.dto.preview;

import com.cms.entity.PreviewJob;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviewJobDto {

    private String id;
    private String status;
    private int attempts;
    private Instant queuedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant generatedAt;

    public static PreviewJobDto from(PreviewJob job) {
        return PreviewJobDto.builder()
                .id(job.getUuid())
                .status(job.getStatus().name())
                .attempts(job.getAttempts() != null ? job.getAttempts() : 0)
                .queuedAt(job.getQueuedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
