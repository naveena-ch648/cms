package com.cms.repository;

import com.cms.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByUuid(String uuid);

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(Long recipientId, Boolean isRead, Pageable pageable);

    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllReadByRecipientId(@Param("recipientId") Long recipientId);
}
