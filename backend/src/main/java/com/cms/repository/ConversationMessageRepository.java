package com.cms.repository;

import com.cms.entity.ConversationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    Optional<ConversationMessage> findByUuid(String uuid);

    Page<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    List<ConversationMessage> findTop10ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    long countByConversationId(Long conversationId);
}
