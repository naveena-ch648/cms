package com.cms.service;

import com.cms.dto.qa.CitationDto;
import com.cms.entity.Conversation;
import com.cms.entity.Conversation.ConversationStatus;
import com.cms.entity.ConversationMessage;
import com.cms.entity.ConversationMessage.Role;
import com.cms.entity.Organization;
import com.cms.entity.User;
import com.cms.entity.Workspace;
import com.cms.repository.ConversationMessageRepository;
import com.cms.repository.ConversationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMessageRepository messageRepository,
                               ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Conversation getOrCreateConversation(String conversationUuid, Long userId,
                                                 Long workspaceId, Long organizationId, String firstQuestion) {
        if (conversationUuid != null && !conversationUuid.isBlank()) {
            return conversationRepository.findByUuid(conversationUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        }

        // Create new conversation
        String title = firstQuestion.length() > 100 ? firstQuestion.substring(0, 100) + "..." : firstQuestion;

        Conversation conversation = Conversation.builder()
                .uuid(UUID.randomUUID().toString())
                .user(User.builder().id(userId).build())
                .workspace(Workspace.builder().id(workspaceId).build())
                .organization(Organization.builder().id(organizationId).build())
                .title(title)
                .build();

        return conversationRepository.save(conversation);
    }

    @Transactional
    public ConversationMessage addMessage(Conversation conversation, Role role, String content,
                                          List<CitationDto> citations, String modelUsed, List<String> chunkIds) {
        String citationsJson = null;
        String chunksJson = null;

        try {
            if (citations != null && !citations.isEmpty()) {
                citationsJson = objectMapper.writeValueAsString(citations);
            }
            if (chunkIds != null && !chunkIds.isEmpty()) {
                chunksJson = objectMapper.writeValueAsString(chunkIds);
            }
        } catch (JsonProcessingException e) {
            // Ignore serialization errors for non-critical metadata
        }

        ConversationMessage message = ConversationMessage.builder()
                .uuid(UUID.randomUUID().toString())
                .conversation(conversation)
                .role(role)
                .content(content)
                .citations(citationsJson)
                .modelUsed(modelUsed)
                .retrievalChunks(chunksJson)
                .build();

        message = messageRepository.save(message);

        // Update message count
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversationRepository.save(conversation);

        // Invalidate conversation context cache (no-op, caching removed)
        // redisTemplate.delete(CONVERSATION_CACHE_PREFIX + conversation.getUuid() + ":context");

        return message;
    }

    /**
     * Get recent conversation context for follow-up questions (last 10 messages).
     * Uses Redis cache with 30-min TTL for active conversations.
     */
    public String getRecentContext(String conversationUuid) {
        Conversation conversation = conversationRepository.findByUuid(conversationUuid).orElse(null);
        if (conversation == null) return "";

        List<ConversationMessage> messages = messageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversation.getId());

        if (messages.isEmpty()) return "";

        Collections.reverse(messages);

        StringBuilder sb = new StringBuilder();
        for (ConversationMessage msg : messages) {
            sb.append(msg.getRole() == Role.USER ? "User: " : "Assistant: ");
            sb.append(msg.getContent()).append("\n");
        }

        return sb.toString();
    }

    public Conversation getConversationByUuid(String uuid) {
        return conversationRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + uuid));
    }

    public Page<Conversation> listConversations(Long userId, Long workspaceId,
                                                 ConversationStatus status, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            if (status != null) {
                return conversationRepository.findByUserIdAndWorkspaceIdAndStatusAndTitleContainingIgnoreCase(
                        userId, workspaceId, status, search, pageable);
            }
            return conversationRepository.findByUserIdAndWorkspaceIdAndTitleContainingIgnoreCase(
                    userId, workspaceId, search, pageable);
        }

        if (status != null) {
            return conversationRepository.findByUserIdAndWorkspaceIdAndStatus(
                    userId, workspaceId, status, pageable);
        }

        return conversationRepository.findByUserIdAndWorkspaceId(userId, workspaceId, pageable);
    }

    public Page<ConversationMessage> getMessages(Long conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
    }

    @Transactional
    public void deleteConversation(String conversationUuid, boolean permanent) {
        Conversation conversation = conversationRepository.findByUuid(conversationUuid)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (permanent) {
            conversationRepository.delete(conversation);
        } else {
            conversation.setStatus(ConversationStatus.ARCHIVED);
            conversationRepository.save(conversation);
        }
    }
}
