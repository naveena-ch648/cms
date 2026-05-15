package com.cms.repository;

import com.cms.entity.FileShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    Optional<FileShare> findByUuid(String uuid);

    /** Find a specific share record (any status) for dedup checks */
    Optional<FileShare> findByFileIdAndSharedWithId(Long fileId, Long sharedWithId);

    /** All active shares for a file — used to list who can access a file */
    @Query("SELECT fs FROM FileShare fs " +
           "JOIN FETCH fs.sharedWith " +
           "WHERE fs.file.id = :fileId AND fs.status = 'ACTIVE'")
    List<FileShare> findActiveByFileId(@Param("fileId") Long fileId);

    /** All shares (any status) for a file — for the file owner to manage */
    @Query("SELECT fs FROM FileShare fs " +
           "JOIN FETCH fs.sharedWith " +
           "WHERE fs.file.id = :fileId")
    List<FileShare> findAllByFileId(@Param("fileId") Long fileId);

    /** All active, non-expired files shared WITH the current user — for Shared Files page */
    @Query("SELECT fs FROM FileShare fs " +
           "JOIN FETCH fs.file f " +
           "JOIN FETCH fs.sharedBy " +
           "WHERE fs.sharedWith.id = :userId " +
           "  AND fs.status = 'ACTIVE' " +
           "  AND f.status = 'ACTIVE' " +
           "  AND (fs.expiresAt IS NULL OR fs.expiresAt > :now) " +
           "ORDER BY fs.createdAt DESC")
    Page<FileShare> findSharedWithUser(@Param("userId") Long userId,
                                       @Param("now") Instant now,
                                       Pageable pageable);

    /** Check if a specific user has an active, non-expired share for a file */
    @Query("SELECT CASE WHEN COUNT(fs) > 0 THEN TRUE ELSE FALSE END " +
           "FROM FileShare fs " +
           "WHERE fs.file.id = :fileId " +
           "  AND fs.sharedWith.id = :userId " +
           "  AND fs.status = 'ACTIVE' " +
           "  AND (fs.expiresAt IS NULL OR fs.expiresAt > CURRENT_TIMESTAMP)")
    boolean existsActiveShareForUser(@Param("fileId") Long fileId, @Param("userId") Long userId);

    /** Get a single active share for permission level check */
    @Query("SELECT fs FROM FileShare fs " +
           "WHERE fs.file.id = :fileId " +
           "  AND fs.sharedWith.id = :userId " +
           "  AND fs.status = 'ACTIVE' " +
           "  AND (fs.expiresAt IS NULL OR fs.expiresAt > CURRENT_TIMESTAMP)")
    Optional<FileShare> findActiveShareForUser(@Param("fileId") Long fileId, @Param("userId") Long userId);
}
