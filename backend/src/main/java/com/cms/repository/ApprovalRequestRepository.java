package com.cms.repository;

import com.cms.entity.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    Optional<ApprovalRequest> findByUuid(String uuid);

    Optional<ApprovalRequest> findByFileIdAndStatus(Long fileId, ApprovalRequest.Status status);

    Page<ApprovalRequest> findByWorkspaceIdAndStatus(Long workspaceId, ApprovalRequest.Status status, Pageable pageable);

    Page<ApprovalRequest> findByWorkspaceId(Long workspaceId, Pageable pageable);

    @Query("SELECT ar FROM ApprovalRequest ar JOIN ApprovalDecision ad ON ad.approvalRequest.id = ar.id " +
           "WHERE ad.reviewer.id = :reviewerId AND ad.decision = 'PENDING' AND ar.status = 'PENDING'")
    Page<ApprovalRequest> findPendingByReviewerId(@Param("reviewerId") Long reviewerId, Pageable pageable);

    Page<ApprovalRequest> findBySubmitterIdAndStatus(Long submitterId, ApprovalRequest.Status status, Pageable pageable);
}
