package com.cms.repository;

import com.cms.entity.UserOrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserOrganizationRoleRepository extends JpaRepository<UserOrganizationRole, UserOrganizationRole.UserOrganizationRoleId> {
    Optional<UserOrganizationRole> findByUserIdAndOrganizationId(Long userId, Long organizationId);
    long countByOrganizationIdAndRoleId(Long organizationId, Long roleId);

    @Query("SELECT uor FROM UserOrganizationRole uor JOIN FETCH uor.role WHERE uor.userId = :userId AND uor.organizationId = :organizationId")
    Optional<UserOrganizationRole> findByUserIdAndOrganizationIdWithRole(@Param("userId") Long userId, @Param("organizationId") Long organizationId);
}
