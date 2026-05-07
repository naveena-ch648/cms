package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sync_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJob {

    public enum Status {
        RUNNING, COMPLETED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "char(36)")
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_link_id", nullable = false)
    private SyncLink syncLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.RUNNING;

    @Column(nullable = false, length = 20)
    private String direction;

    @Column(name = "items_synced", nullable = false)
    @Builder.Default
    private Integer itemsSynced = 0;

    @Column(name = "items_failed", nullable = false)
    @Builder.Default
    private Integer itemsFailed = 0;

    @Column(name = "items_conflicted", nullable = false)
    @Builder.Default
    private Integer itemsConflicted = 0;

    @Column(name = "bytes_transferred", nullable = false)
    @Builder.Default
    private Long bytesTransferred = 0L;

    @Column(name = "error_details", columnDefinition = "JSON")
    private String errorDetails;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
