package com.cms.repository;

import com.cms.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByUuid(String uuid);

    Page<Task> findByFileIdOrderByCreatedAtDesc(Long fileId, Pageable pageable);

    Page<Task> findByFileIdAndStatusOrderByCreatedAtDesc(Long fileId, Task.Status status, Pageable pageable);

    Page<Task> findByAssigneeIdOrderByCreatedAtDesc(Long assigneeId, Pageable pageable);

    Page<Task> findByAssigneeIdAndStatusOrderByCreatedAtDesc(Long assigneeId, Task.Status status, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :assigneeId AND t.status = 'OPEN' AND t.dueDate < CURRENT_DATE ORDER BY t.dueDate ASC")
    Page<Task> findOverdueByAssigneeId(@Param("assigneeId") Long assigneeId, Pageable pageable);

    long countByFileId(Long fileId);
}
