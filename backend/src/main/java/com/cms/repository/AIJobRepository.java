package com.cms.repository;

import com.cms.entity.AIJob;
import com.cms.entity.AIJob.JobStatus;
import com.cms.entity.AIJob.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIJobRepository extends JpaRepository<AIJob, Long> {

    Optional<AIJob> findByUuid(String uuid);

    List<AIJob> findByFileIdAndStatus(Long fileId, JobStatus status);

    List<AIJob> findByFileId(Long fileId);

    Optional<AIJob> findTopByFileIdAndTypeOrderByCreatedAtDesc(Long fileId, JobType type);

    Page<AIJob> findByFileIdOrderByCreatedAtDesc(Long fileId, Pageable pageable);

    List<AIJob> findByFileIdAndStatusIn(Long fileId, List<JobStatus> statuses);

    List<AIJob> findByOrganizationIdAndStatus(Long organizationId, JobStatus status);
}
