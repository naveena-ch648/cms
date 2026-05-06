package com.cms.repository;

import com.cms.entity.FileVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    Page<FileVersion> findByFileIdOrderByVersionNumberDesc(Long fileId, Pageable pageable);

    List<FileVersion> findByFileIdOrderByVersionNumberAsc(Long fileId);

    Optional<FileVersion> findByUuid(String uuid);

    Optional<FileVersion> findByFileIdAndUuid(Long fileId, String uuid);

    Optional<FileVersion> findTopByFileIdOrderByVersionNumberDesc(Long fileId);

    void deleteAllByFileId(Long fileId);
}
