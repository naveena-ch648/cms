package com.cms.service;

import com.cms.entity.AuditEvent;
import com.cms.entity.ComplianceReport;
import com.cms.entity.Organization;
import com.cms.entity.User;
import com.cms.repository.AuditEventRepository;
import com.cms.repository.ComplianceReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceReportService {

    private final ComplianceReportRepository complianceReportRepository;
    private final AuditEventRepository auditEventRepository;

    public ComplianceReport requestReport(Organization org, User requestedBy, String reportType,
                                          Instant dateFrom, Instant dateTo) {
        ComplianceReport report = ComplianceReport.builder()
                .organization(org)
                .requestedBy(requestedBy)
                .reportType(reportType)
                .status("PENDING")
                .dateFrom(dateFrom.atZone(java.time.ZoneOffset.UTC).toLocalDate())
                .dateTo(dateTo.atZone(java.time.ZoneOffset.UTC).toLocalDate())
                .build();
        return complianceReportRepository.save(report);
    }

    @Async
    public void generateReport(Long reportId) {
        ComplianceReport report = complianceReportRepository.findById(reportId).orElse(null);
        if (report == null) return;

        report.setStatus("GENERATING");
        complianceReportRepository.save(report);

        try {
            StringWriter writer = new StringWriter();
            writer.write("Timestamp,Actor,Event Type,Category,Outcome,Resource Type,Resource ID,Resource Name,IP Address,Details\n");

            int page = 0;
            int pageSize = 500;
            long totalEvents = 0;

            while (true) {
                Page<AuditEvent> events = auditEventRepository.findByOrganizationIdAndDateRange(
                        report.getOrganization().getId(),
                        report.getDateFrom().atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                        report.getDateTo().plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                        PageRequest.of(page, pageSize)
                );

                for (AuditEvent event : events.getContent()) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                            event.getCreatedAt(),
                            escapeCsv(event.getActorName()),
                            event.getEventType(),
                            event.getCategory(),
                            event.getOutcome(),
                            event.getResourceType() != null ? event.getResourceType() : "",
                            event.getResourceId() != null ? event.getResourceId() : "",
                            escapeCsv(event.getResourceName()),
                            event.getIpAddress() != null ? event.getIpAddress() : "",
                            escapeCsv(event.getDetails())
                    ));
                    totalEvents++;
                }

                if (!events.hasNext()) break;
                page++;
            }

            String csvContent = writer.toString();
            report.setStatus("COMPLETED");
            report.setTotalEvents((int) totalEvents);
            report.setFilePath(csvContent);
            report.setFileSize((long) csvContent.getBytes().length);
            report.setCompletedAt(Instant.now());
            complianceReportRepository.save(report);

        } catch (Exception e) {
            log.error("Failed to generate compliance report {}", reportId, e);
            report.setStatus("FAILED");
            report.setErrorMessage(e.getMessage());
            complianceReportRepository.save(report);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
