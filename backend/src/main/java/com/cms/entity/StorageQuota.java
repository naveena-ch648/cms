package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "storage_quotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(name = "max_storage_bytes", nullable = false)
    @Builder.Default
    private Long maxStorageBytes = 10737418240L;

    @Column(name = "used_storage_bytes", nullable = false)
    @Builder.Default
    private Long usedStorageBytes = 0L;

    @Column(name = "max_file_size_bytes", nullable = false)
    @Builder.Default
    private Long maxFileSizeBytes = 10737418240L;

    @Column(name = "allowed_extensions", columnDefinition = "JSON")
    private String allowedExtensions;

    @Column(name = "blocked_extensions", columnDefinition = "JSON")
    private String blockedExtensions;

    @Column(name = "trash_retention_days", nullable = false)
    @Builder.Default
    private Integer trashRetentionDays = 30;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
