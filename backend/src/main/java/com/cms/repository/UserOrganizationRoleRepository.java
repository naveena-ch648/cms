package com.cms.repository;

import com.cms.entity.UserOrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOrganizationRoleRepository extends JpaRepository<UserOrganizationRole, UserOrganizationRole.UserOrganizationRoleId> {
    Optional<UserOrganizationRole> findByUserIdAndOrganizationId(Long userId, Long organizationId);
    long countByOrganizationIdAndRoleId(Long organizationId, Long roleId);
}
