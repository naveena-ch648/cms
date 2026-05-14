package com.cms.repository;

import com.cms.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroup.UserGroupId> {
    List<UserGroup> findByGroupId(Long groupId);
    List<UserGroup> findByUserId(Long userId);
    void deleteByUserIdAndGroupId(Long userId, Long groupId);
    void deleteByGroupId(Long groupId);
    boolean existsByUserIdAndGroupId(Long userId, Long groupId);
    long countByGroupId(Long groupId);

    @Query("SELECT ug.groupId FROM UserGroup ug WHERE ug.userId = :userId")
    List<Long> findGroupIdsByUserId(@Param("userId") Long userId);
}
