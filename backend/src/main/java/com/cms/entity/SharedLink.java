package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "shared_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedLink {

    public enum ResourceType {
        FILE, FOLDER
    }

    public enum LinkStatus {
        ACTIVE, REVOKED, EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "char(36)")
    private String uuid;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "allow_download", nullable = false)
    @Builder.Default
    private boolean allowDownload = true;

    @Column(name = "watermark_enabled", nullable = false)
    @Builder.Default
    private boolean watermarkEnabled = false;

    @Column(name = "max_views")
    private Integer maxViews;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LinkStatus status = LinkStatus.ACTIVE;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isAccessible() {
        return status == LinkStatus.ACTIVE && !isExpired();
    }
}
