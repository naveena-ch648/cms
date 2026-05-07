package com.cms.repository;

import com.cms.entity.WorkflowTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTriggerRepository extends JpaRepository<WorkflowTrigger, Long> {

    Optional<WorkflowTrigger> findByUuid(String uuid);

    List<WorkflowTrigger> findByWorkspaceIdAndTriggerStateAndEnabled(Long workspaceId, String triggerState, Boolean enabled);

    List<WorkflowTrigger> findByWorkspaceId(Long workspaceId);

    List<WorkflowTrigger> findByWorkspaceIdAndEnabled(Long workspaceId, Boolean enabled);

    List<WorkflowTrigger> findByWorkspaceIdAndTriggerState(Long workspaceId, String triggerState);
}
