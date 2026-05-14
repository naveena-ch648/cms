package com.cms.service;

import com.cms.dto.qa.AskRequest;
import com.cms.dto.qa.AskResponse;
import com.cms.dto.qa.CitationDto;
import com.cms.dto.qa.SummarizeRequest;
import com.cms.dto.qa.SummarizeResponse;
import com.cms.entity.Conversation;
import com.cms.entity.ConversationMessage;
import com.cms.entity.ConversationMessage.Role;
import com.cms.repository.ConversationMessageRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.lang.Nullable;
import io.qdrant.client.grpc.Points.ScoredPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QAService {

    private static final String SYSTEM_PROMPT = """
            You are a document Q&A assistant. Answer the user's question ONLY using the provided context passages.
            Rules:
            1. If the answer is not found in the context, respond: "I could not find relevant information in your documents to answer this question."
            2. Cite your sources using [1], [2], etc. corresponding to the context passages.
            3. Be concise and factual.
            4. Never make up information not present in the context.
            """;

    private final VectorSearchService vectorSearchService;
    private final ConversationService conversationService;
    private final ConversationMessageRepository messageRepository;
    private final ChatLanguageModel chatLanguageModel;

    public QAService(VectorSearchService vectorSearchService,
                     ConversationService conversationService,
                     ConversationMessageRepository messageRepository,
                     @Nullable ChatLanguageModel chatLanguageModel) {
        this.vectorSearchService = vectorSearchService;
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.chatLanguageModel = chatLanguageModel;
    }

    @Transactional
    public AskResponse ask(AskRequest request, Long userId, Long organizationId,
                           List<String> accessibleDocumentIds, Long workspaceId,
                           List<Float> questionEmbedding) {

        // 1. Retrieve relevant chunks from Qdrant
        int maxChunks = request.getMaxChunks() != null ? Math.min(request.getMaxChunks(), 10) : 5;
        List<ScoredPoint> results;
        try {
            results = vectorSearchService.search(questionEmbedding, accessibleDocumentIds, workspaceId, maxChunks);
        } catch (Exception e) {
            throw new RuntimeException("Vector search failed", e);
        }

        // 2. Check if we have relevant results
        if (results.isEmpty()) {
            Conversation conversation = conversationService.getOrCreateConversation(
                    request.getConversationId(), userId, workspaceId, organizationId, request.getQuestion());

            ConversationMessage userMsg = conversationService.addMessage(
                    conversation, Role.USER, request.getQuestion(), null, null, null);
            ConversationMessage assistantMsg = conversationService.addMessage(
                    conversation, Role.ASSISTANT,
                    "I could not find relevant information in your documents to answer this question.",
                    null, null, null);

            return AskResponse.builder()
                    .conversationId(conversation.getUuid())
                    .messageId(assistantMsg.getUuid())
                    .answer(assistantMsg.getContent())
                    .citations(Collections.emptyList())
                    .noRelevantInfo(true)
                    .build();
        }

        // 3. Build context from retrieved chunks
        StringBuilder contextBuilder = new StringBuilder();
        List<CitationDto> citations = new ArrayList<>();
        List<String> chunkIds = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            ScoredPoint point = results.get(i);
            String chunkText = VectorSearchService.getPayloadString(point, "chunk_text");
            String documentId = VectorSearchService.getPayloadString(point, "document_id");
            String fileName = VectorSearchService.getPayloadString(point, "file_name");
            int pageNumber = VectorSearchService.getPayloadInt(point, "page_number");
            int charStart = VectorSearchService.getPayloadInt(point, "char_start");
            int charEnd = VectorSearchService.getPayloadInt(point, "char_end");

            contextBuilder.append(String.format("[%d] (from '%s', page %d):\n%s\n\n",
                    i + 1, fileName, pageNumber, chunkText));

            String pointId = point.getId().getUuid();
            chunkIds.add(pointId);

            citations.add(CitationDto.builder()
                    .index(i + 1)
                    .documentId(documentId)
                    .documentName(fileName)
                    .pageNumber(pageNumber)
                    .excerpt(chunkText != null && chunkText.length() > 200 ? chunkText.substring(0, 200) + "..." : chunkText)
                    .chunkId(pointId)
                    .charStart(charStart)
                    .charEnd(charEnd)
                    .build());
        }

        // 4. Build conversation history for context (follow-ups)
        String conversationContext = "";
        if (request.getConversationId() != null) {
            conversationContext = conversationService.getRecentContext(request.getConversationId());
        }

        // 5. Call LLM
        String prompt = buildPrompt(request.getQuestion(), contextBuilder.toString(), conversationContext);
        String answer;
        String modelUsed = null;
        int tokenCount = 0;

        if (chatLanguageModel != null) {
            answer = chatLanguageModel.generate(prompt);
            modelUsed = "gpt-4o-mini";
            tokenCount = prompt.length() / 4 + answer.length() / 4; // rough token estimate
        } else {
            // No LLM configured — return context-based fallback
            answer = "LLM not configured. Retrieved context:\n" + contextBuilder;
        }

        // 6. Save conversation and messages
        Conversation conversation = conversationService.getOrCreateConversation(
                request.getConversationId(), userId, workspaceId, organizationId, request.getQuestion());

        conversationService.addMessage(conversation, Role.USER, request.getQuestion(), null, null, null);
        ConversationMessage assistantMsg = conversationService.addMessage(
                conversation, Role.ASSISTANT, answer, citations, modelUsed, chunkIds);

        return AskResponse.builder()
                .conversationId(conversation.getUuid())
                .messageId(assistantMsg.getUuid())
                .answer(answer)
                .citations(citations)
                .modelUsed(modelUsed)
                .tokenCount(tokenCount)
                .noRelevantInfo(false)
                .build();
    }

    private String buildPrompt(String question, String context, String conversationHistory) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");

        if (!conversationHistory.isBlank()) {
            sb.append("Previous conversation:\n").append(conversationHistory).append("\n\n");
        }

        sb.append("Context passages:\n").append(context).append("\n");
        sb.append("User question: ").append(question);

        return sb.toString();
    }

    @Transactional
    public SummarizeResponse summarize(SummarizeRequest request, String documentId,
                                       Long workspaceId, List<Float> questionEmbedding) {
        // Use a summarization-focused query to retrieve all chunks for the document
        List<ScoredPoint> results;
        try {
            results = vectorSearchService.search(questionEmbedding,
                    List.of(documentId), workspaceId, 20);
        } catch (Exception e) {
            throw new RuntimeException("Vector search for summarization failed", e);
        }

        if (results.isEmpty()) {
            return SummarizeResponse.builder()
                    .documentId(documentId)
                    .summary("No content found for this document. It may not have been indexed yet.")
                    .citations(Collections.emptyList())
                    .build();
        }

        // Build context from all retrieved chunks
        StringBuilder contextBuilder = new StringBuilder();
        List<CitationDto> citations = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            ScoredPoint point = results.get(i);
            String chunkText = VectorSearchService.getPayloadString(point, "chunk_text");
            String fileName = VectorSearchService.getPayloadString(point, "file_name");
            int pageNumber = VectorSearchService.getPayloadInt(point, "page_number");
            int charStart = VectorSearchService.getPayloadInt(point, "char_start");
            int charEnd = VectorSearchService.getPayloadInt(point, "char_end");

            contextBuilder.append(String.format("[%d] (page %d):\n%s\n\n", i + 1, pageNumber, chunkText));

            citations.add(CitationDto.builder()
                    .index(i + 1)
                    .documentId(documentId)
                    .documentName(fileName)
                    .pageNumber(pageNumber)
                    .excerpt(chunkText != null && chunkText.length() > 200
                            ? chunkText.substring(0, 200) + "..." : chunkText)
                    .chunkId(point.getId().getUuid())
                    .charStart(charStart)
                    .charEnd(charEnd)
                    .build());
        }

        // Determine length instruction
        String lengthInstruction = switch (request.getLength()) {
            case "short" -> "Provide a brief 2-3 sentence summary.";
            case "long" -> "Provide a detailed summary covering all main points (500+ words).";
            default -> "Provide a moderate summary of about 200-300 words.";
        };

        String prompt = String.format("""
                Summarize the following document content. %s
                Cite sections using [1], [2], etc. corresponding to the passage numbers.
                Focus on key themes, findings, and conclusions.

                Document passages:
                %s
                """, lengthInstruction, contextBuilder);

        String summary;
        String modelUsed = null;
        int tokenCount = 0;

        if (chatLanguageModel != null) {
            summary = chatLanguageModel.generate(prompt);
            modelUsed = "gpt-4o-mini";
            tokenCount = prompt.length() / 4 + summary.length() / 4;
        } else {
            summary = "LLM not configured. Retrieved " + results.size() + " document chunks.";
        }

        return SummarizeResponse.builder()
                .documentId(documentId)
                .documentName(VectorSearchService.getPayloadString(results.get(0), "file_name"))
                .summary(summary)
                .citations(citations)
                .modelUsed(modelUsed)
                .tokenCount(tokenCount)
                .build();
    }
}
