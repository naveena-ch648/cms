package com.cms.service;

import com.cms.dto.digest.EmailDigestPreferenceDto;
import com.cms.dto.digest.UpdateDigestPreferenceRequest;
import com.cms.entity.StorageQuota;
import com.cms.entity.User;
import com.cms.entity.UserEmailPreference;
import com.cms.entity.UserEmailPreference.DigestFrequency;
import com.cms.repository.ApprovalRequestRepository;
import com.cms.repository.FileRepository;
import com.cms.repository.UserEmailPreferenceRepository;
import com.cms.repository.UserWorkspaceRoleRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDigestService {

    private final UserEmailPreferenceRepository preferenceRepository;
    private final UserWorkspaceRoleRepository userWorkspaceRoleRepository;
    private final FileRepository fileRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final StorageQuotaService storageQuotaService;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.digest.from}")
    private String fromAddress;

    @Value("${email.digest.base-url}")
    private String appBaseUrl;

    // ─── Preference management ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EmailDigestPreferenceDto getPreference(Long userId) {
        UserEmailPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> buildDefault(userId));
        return EmailDigestPreferenceDto.from(pref);
    }

    @Transactional
    public EmailDigestPreferenceDto updatePreference(Long userId, User user,
                                                      UpdateDigestPreferenceRequest req) {
        UserEmailPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserEmailPreference p = buildDefault(userId);
                    p.setUser(user);
                    return p;
                });

        if (req.getDigestEnabled() != null) {
            pref.setDigestEnabled(req.getDigestEnabled());
        }
        if (req.getDigestFrequency() != null) {
            pref.setDigestFrequency(DigestFrequency.valueOf(req.getDigestFrequency()));
        }
        if (req.getIncludeSharedFiles() != null) {
            pref.setIncludeSharedFiles(req.getIncludeSharedFiles());
        }
        if (req.getIncludePendingApprovals() != null) {
            pref.setIncludePendingApprovals(req.getIncludePendingApprovals());
        }
        if (req.getIncludeStorageUsage() != null) {
            pref.setIncludeStorageUsage(req.getIncludeStorageUsage());
        }
        if (req.getIncludeRecentActivity() != null) {
            pref.setIncludeRecentActivity(req.getIncludeRecentActivity());
        }

        return EmailDigestPreferenceDto.from(preferenceRepository.save(pref));
    }

    // ─── Digest sending ───────────────────────────────────────────────────────

    /**
     * Called by the scheduler to send digests of the given frequency.
     * Only sends to users whose last digest was sent before the cutoff.
     */
    @Transactional
    public void sendDigests(DigestFrequency frequency) {
        Instant cutoff = frequency == DigestFrequency.DAILY
                ? Instant.now().minus(23, ChronoUnit.HOURS)
                : Instant.now().minus(6, ChronoUnit.DAYS);

        List<UserEmailPreference> due = preferenceRepository.findDueForDigest(frequency, cutoff);
        log.info("Sending {} digest emails for {} users", frequency, due.size());

        for (UserEmailPreference pref : due) {
            try {
                sendDigestEmail(pref);
                pref.setLastDigestSentAt(Instant.now());
                preferenceRepository.save(pref);
            } catch (Exception e) {
                log.error("Failed to send digest email to userId={}: {}", pref.getUser().getId(), e.getMessage(), e);
            }
        }
    }

    private void sendDigestEmail(UserEmailPreference pref) throws Exception {
        User user = pref.getUser();
        Context ctx = new Context(Locale.ENGLISH);

        ctx.setVariable("firstName", user.getFirstName());
        ctx.setVariable("appBaseUrl", appBaseUrl);
        ctx.setVariable("periodLabel", buildPeriodLabel(pref.getDigestFrequency()));

        Instant since = pref.getDigestFrequency() == DigestFrequency.DAILY
                ? Instant.now().minus(1, ChronoUnit.DAYS)
                : Instant.now().minus(7, ChronoUnit.DAYS);

        Long orgId = user.getOrganization().getId();
        List<Long> workspaceIds = userWorkspaceRoleRepository.findWorkspaceIdsByUserId(user.getId());

        // Pending approvals
        if (pref.isIncludePendingApprovals()) {
            long pending = approvalRequestRepository.countPendingForApprover(user.getId());
            ctx.setVariable("pendingApprovals", pending);
        }

        // Recent files
        if (pref.isIncludeRecentActivity() && !workspaceIds.isEmpty()) {
            List<Map<String, Object>> recentFiles = fileRepository
                    .findRecentlyModifiedSummary(workspaceIds, since);
            ctx.setVariable("recentFiles", recentFiles);
            ctx.setVariable("recentActivityCount", recentFiles.size());
        }

        // Storage usage
        if (pref.isIncludeStorageUsage()) {
            try {
                StorageQuota quota = storageQuotaService.getQuotaForOrg(orgId);
                long used = quota.getUsedStorageBytes();
                long max = quota.getMaxStorageBytes();
                double pct = max > 0 ? (double) used / max * 100 : 0;
                ctx.setVariable("storageUsedBytes", used);
                ctx.setVariable("storageMaxBytes", max);
                ctx.setVariable("storagePercentage", pct);
                ctx.setVariable("storageUsedHuman", humanReadableBytes(used));
                ctx.setVariable("storageMaxHuman", humanReadableBytes(max));
            } catch (Exception e) {
                log.debug("No quota found for org {}", orgId);
            }
        }

        String html = templateEngine.process("email/digest", ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(user.getEmail());
        helper.setSubject("Your CMS " + pref.getDigestFrequency().name().toLowerCase() + " digest");
        helper.setText(html, true);

        mailSender.send(message);
        log.info("Digest sent to {}", user.getEmail());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private UserEmailPreference buildDefault(Long userId) {
        return UserEmailPreference.builder()
                .digestEnabled(true)
                .digestFrequency(DigestFrequency.WEEKLY)
                .includeSharedFiles(true)
                .includePendingApprovals(true)
                .includeStorageUsage(true)
                .includeRecentActivity(true)
                .build();
    }

    private String buildPeriodLabel(DigestFrequency freq) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM d, yyyy")
                .withZone(ZoneId.of("UTC"));
        if (freq == DigestFrequency.DAILY) {
            return fmt.format(Instant.now().minus(1, ChronoUnit.DAYS));
        }
        return fmt.format(Instant.now().minus(7, ChronoUnit.DAYS))
                + " – " + fmt.format(Instant.now());
    }

    private String humanReadableBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
