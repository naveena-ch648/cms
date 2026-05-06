package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "char(36)")
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "storage_bucket", nullable = false, length = 63)
    private String storageBucket;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "change_note", length = 500)
    private String changeNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
    }
}
