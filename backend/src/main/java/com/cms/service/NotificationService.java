package com.cms.service;

import com.cms.dto.collaboration.NotificationDto;
import com.cms.entity.Notification;
import com.cms.entity.User;
import com.cms.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final String UNREAD_COUNT_KEY_PREFIX = "notifications:unread:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public Notification createNotification(User recipient, Notification.Type type, String title,
                                           String message, String targetType, String targetId, User actor) {
        Notification notification = Notification.builder()
                .uuid(UUID.randomUUID().toString())
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .targetType(targetType)
                .targetId(targetId)
                .actor(actor)
                .build();

        notification = notificationRepository.save(notification);
        invalidateUnreadCountCache(recipient.getId());
        return notification;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long recipientId, String type, Pageable pageable) {
        if (type != null && !type.isBlank()) {
            try {
                Notification.Type notificationType = Notification.Type.valueOf(type.toUpperCase());
                return notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(recipientId, notificationType, pageable)
                        .map(NotificationDto::from);
            } catch (IllegalArgumentException e) {
                // Invalid type, fall through to unfiltered
            }
        }
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        String key = UNREAD_COUNT_KEY_PREFIX + recipientId;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(cached);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for notification count cache (user {}): {}", recipientId, e.getMessage());
        }

        long count = notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(count), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache notification count for user {}: {}", recipientId, e.getMessage());
        }
        return count;
    }

    @Transactional
    public NotificationDto markAsRead(String notificationUuid, Long recipientId) {
        Notification notification = notificationRepository.findByUuid(notificationUuid)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationUuid));

        if (!notification.getRecipient().getId().equals(recipientId)) {
            throw new SecurityException("Cannot mark another user's notification as read");
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
            invalidateUnreadCountCache(recipientId);
        }

        return NotificationDto.from(notification);
    }

    @Transactional
    public void markAllAsRead(Long recipientId) {
        int updated = notificationRepository.markAllReadByRecipientId(recipientId);
        if (updated > 0) {
            invalidateUnreadCountCache(recipientId);
        }
    }

    private void invalidateUnreadCountCache(Long recipientId) {
        try {
            redisTemplate.delete(UNREAD_COUNT_KEY_PREFIX + recipientId);
        } catch (Exception e) {
            log.warn("Failed to invalidate notification cache for user {}: {}", recipientId, e.getMessage());
        }
    }
}
