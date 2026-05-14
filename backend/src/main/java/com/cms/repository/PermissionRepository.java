package com.cms.repository;

import com.cms.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    List<Permission> findByIdIn(List<Long> ids);
    boolean existsByName(String name);
}
