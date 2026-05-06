package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.collaboration.NotificationDto;
import com.cms.security.UserPrincipal;
import com.cms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        int pageSize = Math.min(size, 50);
        Page<NotificationDto> notifications = notificationService.getNotifications(
                principal.getId(), PageRequest.of(page, pageSize));
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {

        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(
            @PathVariable String notificationId,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationDto notification = notificationService.markAsRead(notificationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(notification));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal) {

        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
