package com.cms.repository;

import com.cms.entity.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, Long> {

    List<ApprovalDecision> findByApprovalRequestId(Long approvalRequestId);

    Optional<ApprovalDecision> findByApprovalRequestIdAndReviewerId(Long approvalRequestId, Long reviewerId);

    long countByApprovalRequestIdAndDecision(Long approvalRequestId, ApprovalDecision.Decision decision);

    long countByReviewerIdAndDecision(Long reviewerId, ApprovalDecision.Decision decision);
}
