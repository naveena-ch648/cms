package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.entity.AuditEvent;
import com.cms.middleware.TenantContext;
import com.cms.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestParam(required = false) String eventType,
            Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<AuditEvent> page;
        if (eventType != null && !eventType.isBlank()) {
            page = auditEventRepository.findByOrganizationIdAndEventType(orgId, eventType, pageable);
        } else {
            page = auditEventRepository.findByOrganizationId(orgId, pageable);
        }

        List<Map<String, Object>> events = page.getContent().stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "eventType", e.getEventType(),
                        "userId", e.getUser() != null ? e.getUser().getId() : 0L,
                        "ipAddress", e.getIpAddress() != null ? e.getIpAddress() : "",
                        "details", e.getDetails() != null ? e.getDetails() : "",
                        "createdAt", e.getCreatedAt().toString()
                ))
                .toList();

        ApiResponse.PagedMeta meta = ApiResponse.PagedMeta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(events, meta));
    }
}
