package org.frankframework.insights.common.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.frankframework.insights.ratelimit.RateLimitService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for cleaning up old rate limit buckets to prevent memory leaks.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class RateLimitCleanupConfiguration {

    private final RateLimitService rateLimitService;

    /**
     * Scheduled cleanup job that runs daily at midnight to remove fully refilled buckets.
     * This prevents memory leaks by removing buckets that are no longer actively rate limiting.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "rateLimitCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void cleanupOldBuckets() {
        log.debug("Starting rate limit bucket cleanup");
        int bucketCountBefore = rateLimitService.getBucketCount();

        rateLimitService.cleanupOldBuckets();

        int bucketCountAfter = rateLimitService.getBucketCount();
        log.debug("Rate limit cleanup completed. Buckets before: {}, after: {}, removed: {}",
                bucketCountBefore, bucketCountAfter, bucketCountBefore - bucketCountAfter);
    }
}
