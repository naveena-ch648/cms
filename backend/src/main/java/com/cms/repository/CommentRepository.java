package com.cms.repository;

import com.cms.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByUuid(String uuid);

    Page<Comment> findByFileIdAndParentIsNullOrderByCreatedAtAsc(Long fileId, Pageable pageable);

    long countByFileId(Long fileId);

    Page<Comment> findByFolderIdAndParentIsNullOrderByCreatedAtAsc(Long folderId, Pageable pageable);

    long countByFolderId(Long folderId);
}
