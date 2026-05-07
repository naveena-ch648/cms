package com.cms.repository;

import com.cms.entity.MetadataValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetadataValueRepository extends JpaRepository<MetadataValue, Long> {

    List<MetadataValue> findByFileId(Long fileId);

    Optional<MetadataValue> findByFileIdAndFieldId(Long fileId, Long fieldId);

    void deleteByFileIdAndFieldId(Long fileId, Long fieldId);

    void deleteByFieldId(Long fieldId);

    List<MetadataValue> findByFileIdIn(List<Long> fileIds);
}
