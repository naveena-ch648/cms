package com.cms.repository;

import com.cms.entity.Preview;
import com.cms.entity.Preview.PreviewStatus;
import com.cms.entity.Preview.PreviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreviewRepository extends JpaRepository<Preview, Long> {

    Optional<Preview> findByUuid(String uuid);

    Optional<Preview> findByFileIdAndTypeAndStatus(Long fileId, PreviewType type, PreviewStatus status);

    List<Preview> findByFileIdAndType(Long fileId, PreviewType type);

    List<Preview> findByFileId(Long fileId);

    Optional<Preview> findByFileIdAndVersionIdAndTypeAndStatus(
            Long fileId, Long versionId, PreviewType type, PreviewStatus status);
}
