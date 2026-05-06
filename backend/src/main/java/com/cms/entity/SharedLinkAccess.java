package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "shared_link_accesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedLinkAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_link_id", nullable = false)
    private SharedLink sharedLink;

    @CreationTimestamp
    @Column(name = "accessed_at", nullable = false, updatable = false)
    private Instant accessedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
