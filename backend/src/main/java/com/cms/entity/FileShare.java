package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_shares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileShare {

    public enum SharePermission {
        VIEWER, EDITOR
    }

    public enum ShareStatus {
        ACTIVE, REVOKED
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
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    private User sharedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWith;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SharePermission permission = SharePermission.VIEWER;

    @Column(name = "allow_download", nullable = false)
    @Builder.Default
    private boolean allowDownload = true;

    @Column(name = "watermark_enabled", nullable = false)
    @Builder.Default
    private boolean watermarkEnabled = false;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShareStatus status = ShareStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isAccessible() {
        return status == ShareStatus.ACTIVE && !isExpired();
    }
}
