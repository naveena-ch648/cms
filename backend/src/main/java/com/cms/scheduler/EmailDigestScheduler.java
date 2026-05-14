package com.cms.scheduler;

import com.cms.entity.UserEmailPreference.DigestFrequency;
import com.cms.service.EmailDigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDigestScheduler {

    private final EmailDigestService emailDigestService;

    /**
     * Sends daily digests every morning at 07:00 UTC.
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "UTC")
    public void sendDailyDigests() {
        log.info("Starting daily digest run");
        emailDigestService.sendDigests(DigestFrequency.DAILY);
    }

    /**
     * Sends weekly digests every Monday at 08:00 UTC.
     */
    @Scheduled(cron = "0 0 8 * * MON", zone = "UTC")
    public void sendWeeklyDigests() {
        log.info("Starting weekly digest run");
        emailDigestService.sendDigests(DigestFrequency.WEEKLY);
    }
}
