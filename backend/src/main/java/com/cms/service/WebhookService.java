package com.cms.service;

import com.cms.dto.webhook.*;
import com.cms.entity.*;
import com.cms.middleware.TenantContext;
import com.cms.repository.*;
import com.cms.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final IntegrationTokenEncryptor tokenEncryptor;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public WebhookResponse createWebhook(WebhookCreateRequest request, UserPrincipal user) {
        Long orgId = TenantContext.getCurrentTenant();

        Organization org = new Organization();
        org.setId(orgId);
        User userEntity = new User();
        userEntity.setId(user.getId());

        Webhook webhook = Webhook.builder()
                .uuid(UUID.randomUUID().toString())
                .organization(org)
                .name(request.getName())
                .url(request.getUrl())
                .secret(request.getSecret() != null ? tokenEncryptor.encrypt(request.getSecret()) : null)
                .eventTypes(String.join(",", request.getEventTypes()))
                .status(Webhook.Status.ACTIVE)
                .createdByUser(userEntity)
                .consecutiveFailures(0)
                .build();

        webhook = webhookRepository.save(webhook);
        return toResponse(webhook, user);
    }

    @Transactional
    public WebhookResponse updateWebhook(String webhookId, WebhookUpdateRequest request, UserPrincipal user) {
        Webhook webhook = webhookRepository.findByUuid(webhookId)
                .orElseThrow(() -> new NoSuchElementException("Webhook not found"));

        if (!webhook.getOrganization().getId().equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Not authorized");
        }

        if (request.getName() != null) webhook.setName(request.getName());
        if (request.getUrl() != null) webhook.setUrl(request.getUrl());
        if (request.getSecret() != null) webhook.setSecret(tokenEncryptor.encrypt(request.getSecret()));
        if (request.getEventTypes() != null) webhook.setEventTypes(String.join(",", request.getEventTypes()));
        if (request.getStatus() != null) webhook.setStatus(Webhook.Status.valueOf(request.getStatus()));

        webhook = webhookRepository.save(webhook);
        return toResponse(webhook, user);
    }

    public Page<WebhookResponse> listWebhooks(UserPrincipal user, Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<Webhook> webhooks = webhookRepository.findByOrganizationId(orgId, pageable);
        return webhooks.map(w -> toResponse(w, user));
    }

    public WebhookResponse getWebhook(String webhookId, UserPrincipal user) {
        Webhook webhook = webhookRepository.findByUuid(webhookId)
                .orElseThrow(() -> new NoSuchElementException("Webhook not found"));

        if (!webhook.getOrganization().getId().equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Not authorized");
        }

        return toResponse(webhook, user);
    }

    @Transactional
    public void deleteWebhook(String webhookId, UserPrincipal user) {
        Webhook webhook = webhookRepository.findByUuid(webhookId)
                .orElseThrow(() -> new NoSuchElementException("Webhook not found"));

        if (!webhook.getOrganization().getId().equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Not authorized");
        }

        webhookRepository.delete(webhook);
    }

    public Page<WebhookDeliveryResponse> getDeliveries(String webhookId, Pageable pageable) {
        Webhook webhook = webhookRepository.findByUuid(webhookId)
                .orElseThrow(() -> new NoSuchElementException("Webhook not found"));

        if (!webhook.getOrganization().getId().equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Not authorized");
        }

        return deliveryRepository.findByWebhookId(webhook.getId(), pageable)
                .map(this::toDeliveryResponse);
    }

    @Transactional
    public WebhookTestResponse testWebhook(String webhookId, UserPrincipal user) {
        Webhook webhook = webhookRepository.findByUuid(webhookId)
                .orElseThrow(() -> new NoSuchElementException("Webhook not found"));

        if (!webhook.getOrganization().getId().equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Not authorized");
        }

        // Queue a test delivery
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> testPayload = Map.of(
                "event", "webhook.test",
                "eventId", eventId,
                "timestamp", Instant.now().toString(),
                "data", Map.of("message", "This is a test webhook delivery")
        );

        try {
            String json = objectMapper.writeValueAsString(testPayload);
            queueDelivery(webhook, "webhook.test", eventId, json);

            return WebhookTestResponse.builder()
                    .delivered(true)
                    .responseStatus(null)
                    .responseTimeMs(null)
                    .responseBody(null)
                    .error(null)
                    .build();
        } catch (Exception e) {
            return WebhookTestResponse.builder()
                    .delivered(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Dispatches an event to all matching webhooks for the current organization.
     */
    public void dispatchEvent(String eventType, String eventId, Object payload) {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) return;

        List<Webhook> webhooks = webhookRepository.findActiveByOrganizationId(orgId);

        for (Webhook webhook : webhooks) {
            List<String> events = Arrays.asList(webhook.getEventTypes().split(","));
            if (events.contains(eventType) || events.contains("*")) {
                try {
                    String json = objectMapper.writeValueAsString(Map.of(
                            "event", eventType,
                            "eventId", eventId,
                            "timestamp", Instant.now().toString(),
                            "data", payload
                    ));
                    queueDelivery(webhook, eventType, eventId, json);
                } catch (Exception e) {
                    log.error("Failed to queue webhook delivery for webhook {}: {}", webhook.getUuid(), e.getMessage());
                }
            }
        }
    }

    private void queueDelivery(Webhook webhook, String eventType, String eventId, String payload) {
        try {
            Map<String, Object> delivery = new HashMap<>();
            delivery.put("webhookId", webhook.getId());
            delivery.put("webhookUuid", webhook.getUuid());
            delivery.put("url", webhook.getUrl());
            delivery.put("secret", webhook.getSecret() != null ? tokenEncryptor.decrypt(webhook.getSecret()) : null);
            delivery.put("eventType", eventType);
            delivery.put("eventId", eventId);
            delivery.put("payload", payload);
            delivery.put("organizationId", webhook.getOrganization().getId());

            String json = objectMapper.writeValueAsString(delivery);
            redisTemplate.opsForList().leftPush("webhook:deliver", json);
        } catch (Exception e) {
            log.error("Failed to queue webhook delivery: {}", e.getMessage());
        }
    }

    public static String computeHmacSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "sha256=" + hex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }

    private WebhookResponse toResponse(Webhook webhook, UserPrincipal user) {
        return WebhookResponse.builder()
                .id(webhook.getUuid())
                .name(webhook.getName())
                .url(webhook.getUrl())
                .eventTypes(Arrays.asList(webhook.getEventTypes().split(",")))
                .status(webhook.getStatus().name())
                .consecutiveFailures(webhook.getConsecutiveFailures())
                .createdBy(WebhookResponse.CreatedByInfo.builder()
                        .id(String.valueOf(webhook.getCreatedByUser().getId()))
                        .name(user.getFirstName() + " " + user.getLastName())
                        .build())
                .createdAt(webhook.getCreatedAt())
                .updatedAt(webhook.getUpdatedAt())
                .build();
    }

    private WebhookDeliveryResponse toDeliveryResponse(WebhookDelivery delivery) {
        return WebhookDeliveryResponse.builder()
                .id(String.valueOf(delivery.getId()))
                .eventType(delivery.getEventType())
                .eventId(delivery.getEventId())
                .status(delivery.getStatus().name())
                .responseStatus(delivery.getResponseStatus())
                .responseTimeMs(delivery.getResponseTimeMs())
                .attemptNumber(delivery.getAttemptNumber())
                .deliveredAt(delivery.getDeliveredAt())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}
