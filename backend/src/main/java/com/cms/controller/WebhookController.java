package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.webhook.*;
import com.cms.security.UserPrincipal;
import com.cms.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<ApiResponse<WebhookResponse>> createWebhook(
            @Valid @RequestBody WebhookCreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        WebhookResponse response = webhookService.createWebhook(request, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WebhookResponse>>> listWebhooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal user) {
        Page<WebhookResponse> webhooks = webhookService.listWebhooks(user,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(webhooks));
    }

    @GetMapping("/{webhookId}")
    public ResponseEntity<ApiResponse<WebhookResponse>> getWebhook(
            @PathVariable String webhookId,
            @AuthenticationPrincipal UserPrincipal user) {
        WebhookResponse response = webhookService.getWebhook(webhookId, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{webhookId}")
    public ResponseEntity<ApiResponse<WebhookResponse>> updateWebhook(
            @PathVariable String webhookId,
            @RequestBody WebhookUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        WebhookResponse response = webhookService.updateWebhook(webhookId, request, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{webhookId}")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(
            @PathVariable String webhookId,
            @AuthenticationPrincipal UserPrincipal user) {
        webhookService.deleteWebhook(webhookId, user);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{webhookId}/deliveries")
    public ResponseEntity<ApiResponse<Page<WebhookDeliveryResponse>>> getDeliveries(
            @PathVariable String webhookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<WebhookDeliveryResponse> deliveries = webhookService.getDeliveries(webhookId,
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(deliveries));
    }

    @PostMapping("/{webhookId}/test")
    public ResponseEntity<ApiResponse<WebhookTestResponse>> testWebhook(
            @PathVariable String webhookId,
            @AuthenticationPrincipal UserPrincipal user) {
        WebhookTestResponse response = webhookService.testWebhook(webhookId, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
