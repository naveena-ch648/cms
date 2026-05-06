package com.cms.repository;

import com.cms.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByUuid(String uuid);
    Optional<Organization> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
