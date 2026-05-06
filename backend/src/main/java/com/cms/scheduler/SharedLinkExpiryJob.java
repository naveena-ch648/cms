package com.cms.scheduler;

import com.cms.entity.SharedLink;
import com.cms.repository.SharedLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SharedLinkExpiryJob {

    private final SharedLinkRepository sharedLinkRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SHARE_CACHE_PREFIX = "share_link:";

    /**
     * Runs every hour to mark expired share links as EXPIRED.
     */
    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void expireLinks() {
        List<SharedLink> expired = sharedLinkRepository
                .findByStatusAndExpiresAtBefore(SharedLink.LinkStatus.ACTIVE, Instant.now());

        if (expired.isEmpty()) return;

        log.info("Expiring {} share links", expired.size());

        for (SharedLink link : expired) {
            link.setStatus(SharedLink.LinkStatus.EXPIRED);
            sharedLinkRepository.save(link);
            // Invalidate cache
            redisTemplate.delete(SHARE_CACHE_PREFIX + link.getToken());
        }

        log.info("Expired {} share links successfully", expired.size());
    }
}
