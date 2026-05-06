package com.cms.repository;

import com.cms.entity.PreviewJob;
import com.cms.entity.PreviewJob.JobStatus;
import com.cms.entity.PreviewJob.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreviewJobRepository extends JpaRepository<PreviewJob, Long> {

    Optional<PreviewJob> findByUuid(String uuid);

    Optional<PreviewJob> findByFileIdAndJobTypeAndStatus(Long fileId, JobType jobType, JobStatus status);

    List<PreviewJob> findByFileIdOrderByQueuedAtDesc(Long fileId);

    Optional<PreviewJob> findTopByFileIdAndJobTypeOrderByQueuedAtDesc(Long fileId, JobType jobType);
}
