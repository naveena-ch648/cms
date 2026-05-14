package com.cms.repository;

import com.cms.entity.UserRecentFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRecentFileRepository extends JpaRepository<UserRecentFile, Long> {

    Optional<UserRecentFile> findByUserIdAndFileId(Long userId, Long fileId);

    /**
     * Fetch a user's recent files ordered newest-first, with file + workspace + folder eagerly loaded.
     */
    @Query("SELECT urf FROM UserRecentFile urf " +
           "JOIN FETCH urf.file f " +
           "JOIN FETCH f.workspace " +
           "JOIN FETCH f.folder " +
           "WHERE urf.user.id = :userId AND f.status = 'ACTIVE' " +
           "ORDER BY urf.lastAccessedAt DESC")
    List<UserRecentFile> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Prune old entries, keeping only the :limit most-recent for the user.
     * Uses a self-join subquery compatible with MySQL.
     */
    @Modifying
    @Query(value = "DELETE FROM user_recent_files " +
                   "WHERE user_id = :userId AND id NOT IN (" +
                   "  SELECT id FROM (" +
                   "    SELECT id FROM user_recent_files " +
                   "    WHERE user_id = :userId " +
                   "    ORDER BY last_accessed_at DESC " +
                   "    LIMIT :lim" +
                   "  ) AS keep" +
                   ")", nativeQuery = true)
    void trimToLimit(@Param("userId") Long userId, @Param("lim") int lim);
}
