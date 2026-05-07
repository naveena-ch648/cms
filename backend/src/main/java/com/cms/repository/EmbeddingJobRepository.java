package com.cms.repository;

import com.cms.entity.EmbeddingJob;
import com.cms.entity.EmbeddingJob.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, Long> {

    Optional<EmbeddingJob> findByUuid(String uuid);

    Optional<EmbeddingJob> findByFileId(Long fileId);

    Optional<EmbeddingJob> findTopByFileIdOrderByCreatedAtDesc(Long fileId);

    boolean existsByFileIdAndStatusIn(Long fileId, java.util.Collection<JobStatus> statuses);
}
