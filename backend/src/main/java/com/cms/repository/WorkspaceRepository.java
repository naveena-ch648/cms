package com.cms.repository;

import com.cms.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findByUuid(String uuid);
    boolean existsByNameAndOrganizationId(String name, Long organizationId);
    Page<Workspace> findByOrganizationId(Long organizationId, Pageable pageable);
    long countByOrganizationIdAndStatusNot(Long organizationId, Workspace.WorkspaceStatus status);
    Page<Workspace> findByOrganizationIdAndStatusNot(Long organizationId, Workspace.WorkspaceStatus status, Pageable pageable);
}
