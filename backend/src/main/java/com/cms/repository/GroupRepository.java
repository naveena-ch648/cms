package com.cms.repository;

import com.cms.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByUuid(String uuid);
    boolean existsByNameAndOrganizationId(String name, Long organizationId);
    Page<Group> findByOrganizationId(Long organizationId, Pageable pageable);
}
