package com.cms.service;

import com.cms.entity.Comment;
import com.cms.entity.Mention;
import com.cms.entity.Notification;
import com.cms.entity.User;
import com.cms.repository.MentionRepository;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\[([a-f0-9\\-]{36})]");

    private final MentionRepository mentionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public List<Mention> processMentions(Comment comment, User author) {
        List<String> mentionedUserUuids = extractMentionUuids(comment.getContent());
        List<Mention> mentions = new ArrayList<>();

        for (String userUuid : mentionedUserUuids) {
            userRepository.findByUuid(userUuid).ifPresent(mentionedUser -> {
                // Don't create duplicate mentions
                if (mentionRepository.existsByCommentIdAndMentionedUserId(comment.getId(), mentionedUser.getId())) {
                    return;
                }

                Mention mention = Mention.builder()
                        .comment(comment)
                        .mentionedUser(mentionedUser)
                        .build();
                mentions.add(mentionRepository.save(mention));

                // Don't notify self-mentions
                if (!mentionedUser.getId().equals(author.getId())) {
                    try {
                        String targetType = comment.getFile() != null ? "FILE" : "FOLDER";
                        String targetId = comment.getFile() != null
                                ? comment.getFile().getUuid()
                                : comment.getFolder().getUuid();

                        notificationService.createNotification(
                                mentionedUser,
                                Notification.Type.MENTION,
                                author.getFirstName() + " " + author.getLastName() + " mentioned you",
                                truncate(comment.getContent(), 200),
                                targetType,
                                targetId,
                                author
                        );
                    } catch (Exception e) {
                        log.warn("Failed to create mention notification for user {}: {}", userUuid, e.getMessage());
                    }
                }
            });
        }
        return mentions;
    }

    public List<String> extractMentionUuids(String content) {
        List<String> uuids = new ArrayList<>();
        if (content == null) return uuids;
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String uuid = matcher.group(1);
            if (!uuids.contains(uuid)) {
                uuids.add(uuid);
            }
        }
        return uuids;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
