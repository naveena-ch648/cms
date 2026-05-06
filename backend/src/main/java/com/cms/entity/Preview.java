package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "previews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preview {

    public enum PreviewType {
        THUMBNAIL, FULL_PREVIEW
    }

    public enum PreviewStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "char(36)")
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private FileVersion version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PreviewStatus status = PreviewStatus.PENDING;

    @Column(name = "storage_bucket")
    private String storageBucket;

    @Column(name = "storage_key_prefix", length = 500)
    private String storageKeyPrefix;

    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

    @Column(name = "page_count")
    @Builder.Default
    private Integer pageCount = 0;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    private Integer width;

    private Integer height;

    @Column(name = "file_size_bytes")
    @Builder.Default
    private Long fileSizeBytes = 0L;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
