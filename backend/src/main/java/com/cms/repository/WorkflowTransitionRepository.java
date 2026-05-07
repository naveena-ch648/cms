package com.cms.repository;

import com.cms.entity.WorkflowTransition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    Page<WorkflowTransition> findByFileIdOrderByCreatedAtDesc(Long fileId, Pageable pageable);
}
