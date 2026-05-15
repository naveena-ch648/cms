package com.cms.repository;

import com.cms.entity.FileTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileTagRepository extends JpaRepository<FileTag, Long> {

    List<FileTag> findByFileId(Long fileId);

    Optional<FileTag> findByFileIdAndNameIgnoreCase(Long fileId, String name);

    long countByFileId(Long fileId);

    boolean existsByFileIdAndNameIgnoreCase(Long fileId, String name);

    List<FileTag> findByFileIdIn(List<Long> fileIds);

    void deleteByFileIdAndNameIgnoreCase(Long fileId, String name);

    List<FileTag> findDistinctByWorkspaceIdOrderByName(Long workspaceId);
}
