package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    public enum FileStatus {
        ACTIVE, TRASHED, DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "char(36)")
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "storage_bucket", nullable = false, length = 63)
    private String storageBucket;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FileStatus status = FileStatus.ACTIVE;

    @Column(name = "trashed_at")
    private Instant trashedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trashed_by")
    private User trashedBy;

    @Column(name = "permanent_delete_at")
    private Instant permanentDeleteAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "upload_completed_at")
    private Instant uploadCompletedAt;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "thumbnail_key", length = 512)
    private String thumbnailKey;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private FileVersion currentVersion;

    @Column(name = "version_count", nullable = false)
    @Builder.Default
    private Integer versionCount = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = FileStatus.ACTIVE;
        }
        if (downloadCount == null) {
            downloadCount = 0;
        }
    }
}
