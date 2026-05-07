package com.cms.controller;

import com.cms.dto.audit.*;
import com.cms.entity.AuditCategory;
import com.cms.entity.AuditEvent;
import com.cms.entity.ComplianceReport;
import com.cms.entity.User;
import com.cms.middleware.TenantContext;
import com.cms.repository.AuditEventRepository;
import com.cms.repository.ComplianceReportRepository;
import com.cms.service.AuditSearchService;
import com.cms.service.AuditAlertService;
import com.cms.service.ComplianceReportService;
import com.cms.service.UserService;
import com.cms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditSearchService auditSearchService;
    private final AuditEventRepository auditEventRepository;
    private final ComplianceReportService complianceReportService;
    private final ComplianceReportRepository complianceReportRepository;
    private final UserService userService;
    private final AuditAlertService auditAlertService;
    private final com.cms.repository.AuditAlertRuleRepository alertRuleRepository;
    private final com.cms.repository.AuditAlertInstanceRepository alertInstanceRepository;

    @GetMapping("/events/search")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<AuditSearchResponse> searchEvents(@ModelAttribute AuditSearchRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Instant dateFrom = request.getDateFrom() != null ? Instant.parse(request.getDateFrom()) : null;
        Instant dateTo = request.getDateTo() != null ? Instant.parse(request.getDateTo()) : null;

        AuditSearchService.SearchResult result = auditSearchService.search(
                orgId, request.getQuery(), request.getCategory(), request.getEventType(),
                request.getUserId(), request.getOutcome(), request.getWorkspaceId(),
                dateFrom, dateTo, request.getPage(), request.getSize()
        );

        List<AuditEventDto> events = result.hits().stream()
                .map(this::mapHitToDto)
                .toList();

        return ResponseEntity.ok(AuditSearchResponse.builder()
                .events(events)
                .total(result.total())
                .page(result.page())
                .size(result.size())
                .build());
    }

    @GetMapping("/events/{id}")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<AuditEventDetailDto> getEvent(@PathVariable Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        return auditEventRepository.findByIdAndOrganizationId(id, orgId)
                .map(this::toDetailDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<AuditStatsDto> getStats(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        Long orgId = TenantContext.getCurrentTenant();
        Instant from = dateFrom != null ? Instant.parse(dateFrom) : Instant.now().minusSeconds(86400L * 30);
        Instant to = dateTo != null ? Instant.parse(dateTo) : Instant.now();

        long total = auditEventRepository.countByOrganizationIdAndDateRange(orgId, from, to);

        Map<AuditCategory, Long> byCategory = new EnumMap<>(AuditCategory.class);
        for (Object[] row : auditEventRepository.countByOrganizationIdAndDateRangeGroupByCategory(orgId, from, to)) {
            if (row[0] != null) {
                byCategory.put((AuditCategory) row[0], (Long) row[1]);
            }
        }

        Map<String, Long> byOutcome = new HashMap<>();
        for (Object[] row : auditEventRepository.countByOrganizationIdAndDateRangeGroupByOutcome(orgId, from, to)) {
            if (row[0] != null) {
                byOutcome.put((String) row[0], (Long) row[1]);
            }
        }

        return ResponseEntity.ok(AuditStatsDto.builder()
                .totalEvents(total)
                .byCategory(byCategory)
                .byOutcome(byOutcome)
                .build());
    }

    // --- Compliance Reports ---

    @PostMapping("/reports")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<ComplianceReportDto> createReport(
            @RequestBody ComplianceReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userService.getByIdInternal(principal.getId());
        Instant dateFrom = Instant.parse(request.getDateFrom());
        Instant dateTo = Instant.parse(request.getDateTo());

        ComplianceReport report = complianceReportService.requestReport(
                user.getOrganization(), user, request.getReportType(), dateFrom, dateTo);
        complianceReportService.generateReport(report.getId());

        return ResponseEntity.ok(toReportDto(report));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<java.util.List<ComplianceReportDto>> listReports() {
        Long orgId = TenantContext.getCurrentTenant();
        var reports = complianceReportRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, org.springframework.data.domain.PageRequest.of(0, 100));
        var dtos = reports.stream().map(this::toReportDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/reports/{uuid}/download")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String uuid) {
        Long orgId = TenantContext.getCurrentTenant();
        ComplianceReport report = complianceReportRepository.findByUuidAndOrganizationId(uuid, orgId)
                .orElse(null);
        if (report == null || !"COMPLETED".equals(report.getStatus()) || report.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = report.getFilePath().getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-report-" + uuid + ".csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    private ComplianceReportDto toReportDto(ComplianceReport report) {
        return ComplianceReportDto.builder()
                .id(report.getUuid())
                .reportType(report.getReportType())
                .status(report.getStatus())
                .dateFrom(report.getDateFrom() != null ? report.getDateFrom().toString() : null)
                .dateTo(report.getDateTo() != null ? report.getDateTo().toString() : null)
                .totalEvents(report.getTotalEvents())
                .fileSize(report.getFileSize())
                .errorMessage(report.getErrorMessage())
                .createdAt(report.getCreatedAt() != null ? report.getCreatedAt().toString() : null)
                .completedAt(report.getCompletedAt() != null ? report.getCompletedAt().toString() : null)
                .build();
    }

    private AuditEventDto mapHitToDto(Map<String, Object> hit) {
        return AuditEventDto.builder()
                .id(toLong(hit.get("id")))
                .userId(toLong(hit.get("user_id")))
                .actorName((String) hit.get("actor_name"))
                .eventType((String) hit.get("event_type"))
                .category(hit.get("category") != null ? AuditCategory.valueOf((String) hit.get("category")) : null)
                .resourceType((String) hit.get("resource_type"))
                .resourceId(toLong(hit.get("resource_id")))
                .resourceName((String) hit.get("resource_name"))
                .outcome((String) hit.get("outcome"))
                .ipAddress((String) hit.get("ip_address"))
                .createdAt((String) hit.get("created_at"))
                .build();
    }

    private AuditEventDetailDto toDetailDto(AuditEvent event) {
        return AuditEventDetailDto.builder()
                .id(event.getId())
                .userId(event.getUser() != null ? event.getUser().getId() : null)
                .actorName(event.getActorName())
                .eventType(event.getEventType())
                .category(event.getCategory())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .outcome(event.getOutcome())
                .resourceName(event.getResourceName())
                .details(event.getDetails())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .workspaceId(event.getWorkspace() != null ? event.getWorkspace().getId() : null)
                .createdAt(event.getCreatedAt() != null ? event.getCreatedAt().toString() : null)
                .build();
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    // --- Alert Rules ---

    @GetMapping("/alerts/rules")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<java.util.List<AuditAlertRuleDto>> listAlertRules() {
        Long orgId = TenantContext.getCurrentTenant();
        var rules = alertRuleRepository.findByOrganizationId(orgId);
        var dtos = rules.stream().map(this::toRuleDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/alerts/rules")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<AuditAlertRuleDto> createAlertRule(
            @RequestBody java.util.Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userService.getByIdInternal(principal.getId());
        com.cms.entity.AuditAlertRule rule = com.cms.entity.AuditAlertRule.builder()
                .organization(user.getOrganization())
                .name((String) body.get("name"))
                .description((String) body.get("description"))
                .eventType((String) body.get("eventType"))
                .thresholdCount((Integer) body.get("thresholdCount"))
                .timeWindowMinutes((Integer) body.get("timeWindowMinutes"))
                .enabled(body.get("enabled") != null ? (Boolean) body.get("enabled") : true)
                .createdBy(user)
                .build();
        rule = alertRuleRepository.save(rule);
        return ResponseEntity.ok(toRuleDto(rule));
    }

    @PutMapping("/alerts/rules/{uuid}")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<AuditAlertRuleDto> updateAlertRule(
            @PathVariable String uuid,
            @RequestBody java.util.Map<String, Object> body) {

        Long orgId = TenantContext.getCurrentTenant();
        com.cms.entity.AuditAlertRule rule = alertRuleRepository.findByUuidAndOrganizationId(uuid, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        if (body.containsKey("name")) rule.setName((String) body.get("name"));
        if (body.containsKey("description")) rule.setDescription((String) body.get("description"));
        if (body.containsKey("eventType")) rule.setEventType((String) body.get("eventType"));
        if (body.containsKey("thresholdCount")) rule.setThresholdCount((Integer) body.get("thresholdCount"));
        if (body.containsKey("timeWindowMinutes")) rule.setTimeWindowMinutes((Integer) body.get("timeWindowMinutes"));
        if (body.containsKey("enabled")) rule.setEnabled((Boolean) body.get("enabled"));

        rule = alertRuleRepository.save(rule);
        return ResponseEntity.ok(toRuleDto(rule));
    }

    @DeleteMapping("/alerts/rules/{uuid}")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<Void> deleteAlertRule(@PathVariable String uuid) {
        Long orgId = TenantContext.getCurrentTenant();
        com.cms.entity.AuditAlertRule rule = alertRuleRepository.findByUuidAndOrganizationId(uuid, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));
        alertRuleRepository.delete(rule);
        return ResponseEntity.noContent().build();
    }

    // --- Alert Instances ---

    @GetMapping("/alerts/instances")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<org.springframework.data.domain.Page<AuditAlertInstanceDto>> listAlertInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean acknowledged) {

        Long orgId = TenantContext.getCurrentTenant();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<com.cms.entity.AuditAlertInstance> instances;

        if (acknowledged != null) {
            instances = alertInstanceRepository.findByOrganizationIdAndAcknowledgedOrderByCreatedAtDesc(orgId, acknowledged, pageable);
        } else {
            instances = alertInstanceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable);
        }

        return ResponseEntity.ok(instances.map(this::toInstanceDto));
    }

    @PostMapping("/alerts/instances/{uuid}/acknowledge")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long orgId = TenantContext.getCurrentTenant();
        User user = userService.getByIdInternal(principal.getId());
        auditAlertService.acknowledgeAlert(uuid, orgId, user);
        return ResponseEntity.ok().build();
    }

    private AuditAlertRuleDto toRuleDto(com.cms.entity.AuditAlertRule rule) {
        return AuditAlertRuleDto.builder()
                .id(rule.getUuid())
                .name(rule.getName())
                .description(rule.getDescription())
                .eventType(rule.getEventType())
                .thresholdCount(rule.getThresholdCount())
                .timeWindowMinutes(rule.getTimeWindowMinutes())
                .enabled(rule.getEnabled())
                .createdAt(rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null)
                .build();
    }

    private AuditAlertInstanceDto toInstanceDto(com.cms.entity.AuditAlertInstance instance) {
        return AuditAlertInstanceDto.builder()
                .id(instance.getUuid())
                .ruleName(instance.getRule() != null ? instance.getRule().getName() : null)
                .ruleId(instance.getRule() != null ? instance.getRule().getUuid() : null)
                .triggeredByUser(instance.getTriggeredByUser() != null ? instance.getTriggeredByUser().getEmail() : null)
                .eventCount(instance.getEventCount())
                .windowStart(instance.getWindowStart() != null ? instance.getWindowStart().toString() : null)
                .windowEnd(instance.getWindowEnd() != null ? instance.getWindowEnd().toString() : null)
                .acknowledged(instance.getAcknowledged())
                .acknowledgedBy(instance.getAcknowledgedBy() != null ? instance.getAcknowledgedBy().getEmail() : null)
                .acknowledgedAt(instance.getAcknowledgedAt() != null ? instance.getAcknowledgedAt().toString() : null)
                .createdAt(instance.getCreatedAt() != null ? instance.getCreatedAt().toString() : null)
                .build();
    }
}
