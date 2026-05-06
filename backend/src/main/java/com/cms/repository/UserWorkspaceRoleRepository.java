package com.cms.repository;

import com.cms.entity.UserWorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserWorkspaceRoleRepository extends JpaRepository<UserWorkspaceRole, UserWorkspaceRole.UserWorkspaceRoleId> {
    Optional<UserWorkspaceRole> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    List<UserWorkspaceRole> findByWorkspaceId(Long workspaceId);
    List<UserWorkspaceRole> findByUserId(Long userId);
    void deleteByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    @Query("SELECT uwr.workspace.id FROM UserWorkspaceRole uwr WHERE uwr.userId = :userId")
    List<Long> findWorkspaceIdsByUserId(@Param("userId") Long userId);
}
