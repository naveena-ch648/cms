package com.cms.repository;

import com.cms.entity.Conversation;
import com.cms.entity.Conversation.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUuid(String uuid);

    Page<Conversation> findByUserIdAndWorkspaceIdAndStatus(
            Long userId, Long workspaceId, ConversationStatus status, Pageable pageable);

    Page<Conversation> findByUserIdAndWorkspaceId(
            Long userId, Long workspaceId, Pageable pageable);

    Page<Conversation> findByUserIdAndWorkspaceIdAndTitleContainingIgnoreCase(
            Long userId, Long workspaceId, String search, Pageable pageable);

    Page<Conversation> findByUserIdAndWorkspaceIdAndStatusAndTitleContainingIgnoreCase(
            Long userId, Long workspaceId, ConversationStatus status, String search, Pageable pageable);
}
