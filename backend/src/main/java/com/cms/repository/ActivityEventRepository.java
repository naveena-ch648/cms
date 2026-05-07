package com.cms.repository;

import com.cms.entity.ActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    Page<ActivityEvent> findByWorkspaceIdInOrderByCreatedAtDesc(List<Long> workspaceIds, Pageable pageable);

    Page<ActivityEvent> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    Page<ActivityEvent> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);
}
