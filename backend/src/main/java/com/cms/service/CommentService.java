package com.cms.service;

import com.cms.dto.preview.CommentDto;
import com.cms.entity.Comment;
import com.cms.entity.FileEntity;
import com.cms.entity.Folder;
import com.cms.entity.User;
import com.cms.repository.CommentRepository;
import com.cms.repository.FileRepository;
import com.cms.repository.FolderRepository;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final MentionService mentionService;
    private final AuditService auditService;
    private final ActivityEventService activityEventService;

    @Transactional(readOnly = true)
    public Page<CommentDto> getComments(Long fileId, Pageable pageable) {
        return commentRepository.findByFileIdAndParentIsNullOrderByCreatedAtAsc(fileId, pageable)
                .map(CommentDto::from);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> getFolderComments(Long folderId, Pageable pageable) {
        return commentRepository.findByFolderIdAndParentIsNullOrderByCreatedAtAsc(folderId, pageable)
                .map(CommentDto::from);
    }

    @Transactional(readOnly = true)
    public long getCommentCount(Long fileId) {
        return commentRepository.countByFileId(fileId);
    }

    @Transactional(readOnly = true)
    public long getFolderCommentCount(Long folderId) {
        return commentRepository.countByFolderId(folderId);
    }

    @Transactional
    public CommentDto createComment(FileEntity file, User user, String content, String parentUuid) {
        if (content == null || content.isBlank() || content.length() > 5000) {
            throw new IllegalArgumentException("Comment content must be 1-5000 characters");
        }

        Comment parent = null;
        if (parentUuid != null && !parentUuid.isBlank()) {
            parent = commentRepository.findByUuid(parentUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found: " + parentUuid));

            if (file != null && parent.getFile() != null && !parent.getFile().getId().equals(file.getId())) {
                throw new IllegalArgumentException("Parent comment does not belong to this file");
            }

            if (parent.getParent() != null) {
                throw new IllegalArgumentException("Cannot reply to a reply (max 2 levels)");
            }
        }

        Comment comment = Comment.builder()
                .uuid(UUID.randomUUID().toString())
                .file(file)
                .user(user)
                .parent(parent)
                .content(content.trim())
                .build();

        comment = commentRepository.save(comment);

        // Process mentions
        try {
            mentionService.processMentions(comment, user);
        } catch (Exception e) {
            log.warn("Failed to process mentions for comment {}: {}", comment.getUuid(), e.getMessage());
        }

        // Audit log
        try {
            auditService.log(user.getOrganization(), user, "COMMENT_CREATED", "Comment",
                    comment.getId(), null, null);
        } catch (Exception e) {
            log.warn("Failed to log audit event for comment creation: {}", e.getMessage());
        }

        // Record activity event
        if (file != null) {
            try {
                activityEventService.recordEvent(user, com.cms.entity.ActivityEvent.ActionType.COMMENT_ADDED,
                        "FILE", file.getUuid(), file.getName(),
                        file.getWorkspace(), file.getWorkspace().getOrganization(), null);
            } catch (Exception e) {
                log.warn("Failed to record activity event for comment: {}", e.getMessage());
            }
        }

        return CommentDto.from(comment);
    }

    @Transactional
    public CommentDto createFolderComment(Folder folder, User user, String content, String parentUuid) {
        if (content == null || content.isBlank() || content.length() > 5000) {
            throw new IllegalArgumentException("Comment content must be 1-5000 characters");
        }

        Comment parent = null;
        if (parentUuid != null && !parentUuid.isBlank()) {
            parent = commentRepository.findByUuid(parentUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found: " + parentUuid));

            if (folder != null && parent.getFolder() != null && !parent.getFolder().getId().equals(folder.getId())) {
                throw new IllegalArgumentException("Parent comment does not belong to this folder");
            }

            if (parent.getParent() != null) {
                throw new IllegalArgumentException("Cannot reply to a reply (max 2 levels)");
            }
        }

        Comment comment = Comment.builder()
                .uuid(UUID.randomUUID().toString())
                .folder(folder)
                .user(user)
                .parent(parent)
                .content(content.trim())
                .build();

        comment = commentRepository.save(comment);

        // Process mentions
        try {
            mentionService.processMentions(comment, user);
        } catch (Exception e) {
            log.warn("Failed to process mentions for comment {}: {}", comment.getUuid(), e.getMessage());
        }

        // Audit log
        try {
            auditService.log(user.getOrganization(), user, "COMMENT_CREATED", "Comment",
                    comment.getId(), null, null);
        } catch (Exception e) {
            log.warn("Failed to log audit event for comment creation: {}", e.getMessage());
        }

        return CommentDto.from(comment);
    }

    @Transactional
    public void deleteComment(String commentUuid, Long userId) {
        Comment comment = commentRepository.findByUuid(commentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentUuid));

        if (!comment.getUser().getId().equals(userId)) {
            throw new SecurityException("Only the author can delete their comment");
        }

        User user = comment.getUser();
        commentRepository.delete(comment);

        // Audit log
        try {
            auditService.log(user.getOrganization(), user, "COMMENT_DELETED", "Comment",
                    comment.getId(), null, null);
        } catch (Exception e) {
            log.warn("Failed to log audit event for comment deletion: {}", e.getMessage());
        }
    }
}
