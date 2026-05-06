package com.cms.repository;

import com.cms.entity.SharedLinkAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedLinkAccessRepository extends JpaRepository<SharedLinkAccess, Long> {

    Page<SharedLinkAccess> findBySharedLinkId(Long sharedLinkId, Pageable pageable);
}
