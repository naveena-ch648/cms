package com.cms.repository;

import com.cms.entity.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    Page<WebhookDelivery> findByWebhookId(Long webhookId, Pageable pageable);

    Page<WebhookDelivery> findByWebhookIdAndStatus(Long webhookId, WebhookDelivery.Status status, Pageable pageable);

    Page<WebhookDelivery> findByWebhookIdAndEventType(Long webhookId, String eventType, Pageable pageable);

    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(WebhookDelivery.Status status, Instant now);

    Optional<WebhookDelivery> findByEventId(String eventId);
}
