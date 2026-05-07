package com.cms.service;

import com.cms.dto.dashboard.AlertDto;
import com.cms.entity.SharedLink;
import com.cms.entity.StorageQuota;
import com.cms.entity.User;
import com.cms.entity.UserAlert;
import com.cms.repository.SharedLinkRepository;
import com.cms.repository.UserAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final UserAlertRepository userAlertRepository;
    private final StorageQuotaService storageQuotaService;
    private final SharedLinkRepository sharedLinkRepository;

    @Transactional
    public void generateAlerts(User user, Long organizationId) {
        try {
            generateStorageAlerts(user, organizationId);
            generateLinkExpiryAlerts(user);
        } catch (Exception e) {
            log.error("Failed to generate alerts for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private void generateStorageAlerts(User user, Long organizationId) {
        try {
            StorageQuota quota = storageQuotaService.getQuotaForOrg(organizationId);
            double percentage = quota.getMaxStorageBytes() > 0
                    ? (double) quota.getUsedStorageBytes() / quota.getMaxStorageBytes() * 100
                    : 0;

            if (percentage >= 95) {
                createAlertIfNotExists(user, UserAlert.AlertType.STORAGE_CRITICAL, UserAlert.Severity.CRITICAL,
                        "Storage Critical",
                        String.format("Storage usage is at %.0f%%. Please free up space or upgrade your plan.", percentage),
                        "ORGANIZATION", organizationId.toString());
            } else if (percentage >= 80) {
                createAlertIfNotExists(user, UserAlert.AlertType.STORAGE_WARNING, UserAlert.Severity.WARNING,
                        "Storage Warning",
                        String.format("Storage usage is at %.0f%%. Consider freeing up space.", percentage),
                        "ORGANIZATION", organizationId.toString());
            }
        } catch (IllegalStateException e) {
            // No quota configured — skip
        }
    }

    private void generateLinkExpiryAlerts(User user) {
        Instant in24Hours = Instant.now().plus(24, ChronoUnit.HOURS);
        List<SharedLink> expiringLinks = sharedLinkRepository.findByStatusAndExpiresAtBefore(
                SharedLink.LinkStatus.ACTIVE, in24Hours);

        for (SharedLink link : expiringLinks) {
            if (link.getCreatedBy() != null && link.getCreatedBy().getId().equals(user.getId())) {
                String fileName = link.getFile() != null ? link.getFile().getName() : "Unknown";
                createAlertIfNotExists(user, UserAlert.AlertType.LINK_EXPIRING, UserAlert.Severity.WARNING,
                        "Shared Link Expiring",
                        String.format("Your shared link for '%s' will expire soon.", fileName),
                        "SHARED_LINK", link.getUuid());
            }
        }
    }

    private void createAlertIfNotExists(User user, UserAlert.AlertType alertType, UserAlert.Severity severity,
                                        String title, String message, String targetType, String targetId) {
        List<UserAlert> existing = userAlertRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(user.getId());
        boolean alreadyExists = existing.stream()
                .anyMatch(a -> a.getAlertType() == alertType
                        && targetId.equals(a.getTargetId()));
        if (!alreadyExists) {
            UserAlert alert = UserAlert.builder()
                    .user(user)
                    .alertType(alertType)
                    .severity(severity)
                    .title(title)
                    .message(message)
                    .targetType(targetType)
                    .targetId(targetId)
                    .build();
            userAlertRepository.save(alert);
        }
    }

    @Transactional
    public void dismiss(String alertUuid, Long userId) {
        UserAlert alert = userAlertRepository.findByUuid(alertUuid)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertUuid));
        if (!alert.getUser().getId().equals(userId)) {
            throw new SecurityException("Cannot dismiss another user's alert");
        }
        alert.setDismissed(true);
        alert.setDismissedAt(Instant.now());
        userAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<AlertDto> getActiveAlerts(Long userId) {
        return userAlertRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(AlertDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getActiveAlertCount(Long userId) {
        return userAlertRepository.countByUserIdAndDismissedFalse(userId);
    }
}
