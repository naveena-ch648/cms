package com.cms.repository;

import com.cms.entity.GroupWorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupWorkspaceRoleRepository extends JpaRepository<GroupWorkspaceRole, GroupWorkspaceRole.GroupWorkspaceRoleId> {
    Optional<GroupWorkspaceRole> findByGroupIdAndWorkspaceId(Long groupId, Long workspaceId);
    List<GroupWorkspaceRole> findByGroupId(Long groupId);
    List<GroupWorkspaceRole> findByWorkspaceId(Long workspaceId);
    void deleteByGroupIdAndWorkspaceId(Long groupId, Long workspaceId);

    @Query("SELECT gwr FROM GroupWorkspaceRole gwr JOIN com.cms.entity.UserGroup ug ON ug.groupId = gwr.groupId WHERE ug.userId = :userId AND gwr.workspaceId = :workspaceId")
    List<GroupWorkspaceRole> findByUserIdAndWorkspaceId(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId);
}
