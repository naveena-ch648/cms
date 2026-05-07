package com.cms.repository;

import com.cms.entity.SyncJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {

    Optional<SyncJob> findByUuid(String uuid);

    Page<SyncJob> findBySyncLinkIdOrderByStartedAtDesc(Long syncLinkId, Pageable pageable);
}
