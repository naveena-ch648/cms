package com.cms.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UploadSessionCleanupJob {

    private final JdbcTemplate pgJdbc;

    public UploadSessionCleanupJob(@Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc) {
        this.pgJdbc = pgJdbc;
    }

    @Scheduled(fixedRate = 3600000) // every hour
    public void cleanupExpiredSessions() {
        try {
            int deleted = pgJdbc.update("""
                    DELETE FROM upload_session_parts
                    WHERE session_id IN (
                        SELECT session_id FROM upload_sessions WHERE expires_at < NOW()
                    )
                    """);
            int sessions = pgJdbc.update("DELETE FROM upload_sessions WHERE expires_at < NOW()");
            if (sessions > 0) {
                log.info("Cleaned up {} expired upload sessions ({} parts)", sessions, deleted);
            }
        } catch (Exception e) {
            log.warn("Error during upload session cleanup: {}", e.getMessage());
        }
    }
}
