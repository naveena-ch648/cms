package com.cms.repository;

import com.cms.entity.MetadataField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetadataFieldRepository extends JpaRepository<MetadataField, Long> {

    Optional<MetadataField> findByUuid(String uuid);

    List<MetadataField> findByWorkspaceIdAndDeletedAtIsNullOrderByDisplayOrder(Long workspaceId);

    List<MetadataField> findByWorkspaceIdOrderByDisplayOrder(Long workspaceId);

    long countByWorkspaceIdAndDeletedAtIsNull(Long workspaceId);

    boolean existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(Long workspaceId, String name);

    boolean existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(Long workspaceId, String name, Long id);
}
