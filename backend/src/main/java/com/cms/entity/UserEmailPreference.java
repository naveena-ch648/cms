package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_email_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEmailPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "digest_enabled", nullable = false)
    @Builder.Default
    private boolean digestEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "digest_frequency", nullable = false)
    @Builder.Default
    private DigestFrequency digestFrequency = DigestFrequency.WEEKLY;

    @Column(name = "include_shared_files", nullable = false)
    @Builder.Default
    private boolean includeSharedFiles = true;

    @Column(name = "include_pending_approvals", nullable = false)
    @Builder.Default
    private boolean includePendingApprovals = true;

    @Column(name = "include_storage_usage", nullable = false)
    @Builder.Default
    private boolean includeStorageUsage = true;

    @Column(name = "include_recent_activity", nullable = false)
    @Builder.Default
    private boolean includeRecentActivity = true;

    @Column(name = "last_digest_sent_at")
    private Instant lastDigestSentAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum DigestFrequency {
        DAILY, WEEKLY
    }
}
