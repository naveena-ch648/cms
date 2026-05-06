package com.cms.repository;

import com.cms.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByUuid(String uuid);
    Optional<Role> findByNameAndOrganizationId(String name, Long organizationId);
    boolean existsByNameAndOrganizationId(String name, Long organizationId);
    Page<Role> findByOrganizationId(Long organizationId, Pageable pageable);
    List<Role> findByOrganizationId(Long organizationId);
    List<Role> findByParentRoleId(Long parentRoleId);
}
