package com.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "folder_recents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderRecent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;

    @PrePersist
    public void prePersist() {
        if (accessedAt == null) accessedAt = Instant.now();
    }
}
