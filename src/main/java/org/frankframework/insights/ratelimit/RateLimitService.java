package org.frankframework.insights.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RateLimitService {

    // Unified rate limit: 5 failed attempts per 15 minutes
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int REFILL_DURATION_MINUTES = 15;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Checks if a user is currently blocked due to too many failed requests.
     * Does NOT consume a token - just checks availability.
     *
     * @param userKey The user identifier
     * @throws RateLimitExceededException if the user is blocked
     */
    public void checkIfBlocked(String userKey) throws RateLimitExceededException {
        if (userKey == null) {
            return;
        }
        Bucket bucket = buckets.get(userKey);
        if (bucket != null && bucket.getAvailableTokens() == 0) {
            log.warn("User {} is blocked due to too many failed requests", userKey);
            throw new RateLimitExceededException(
                    "Too many failed requests. Please try again in " + REFILL_DURATION_MINUTES + " minutes.");
        }
    }

    /**
     * Tracks a failed request for the given user.
     * Consumes a token from the bucket.
     *
     * @param userKey The user identifier
     */
    public void trackFailedRequest(String userKey) {
        if (userKey == null) {
            return;
        }
        Bucket bucket = resolveBucket(userKey);
        boolean consumed = bucket.tryConsume(1);

        long availableTokens = bucket.getAvailableTokens();
        log.debug(
                "Failed request tracked for user: {}, Consumed: {}, Remaining attempts: {}",
                userKey,
                consumed,
                availableTokens);

        if (!consumed) {
            log.warn("User {} has exceeded rate limit (0 attempts remaining)", userKey);
        }
    }

    /**
     * Resets the rate limit for a given user (called after successful request).
     *
     * @param userKey The user identifier
     */
    public void resetRateLimit(String userKey) {
        if (userKey == null) {
            return;
        }

        if (buckets.remove(userKey) != null) {
            log.info("Rate limit reset for user: {} after successful request", userKey);
        }
    }

    /**
     * Resolves or creates a bucket for the given user.
     */
    private Bucket resolveBucket(String userKey) {
        return buckets.computeIfAbsent(userKey, k -> {
            log.debug(
                    "Creating new rate limit bucket for user: {} with {} attempts per {} minutes",
                    userKey,
                    MAX_FAILED_ATTEMPTS,
                    REFILL_DURATION_MINUTES);
            return createNewBucket();
        });
    }

    /**
     * Creates a new bucket with the configured rate limit.
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_FAILED_ATTEMPTS)
                .refillIntervally(MAX_FAILED_ATTEMPTS, Duration.ofMinutes(REFILL_DURATION_MINUTES))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Cleanup old buckets to prevent memory leaks.
     * Removes buckets that are fully refilled (no longer actively rate limiting).
     */
    public void cleanupOldBuckets() {
        int removedCount = 0;
        for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
            long availableTokens = entry.getValue().getAvailableTokens();

            if (availableTokens >= MAX_FAILED_ATTEMPTS) {
                buckets.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} fully refilled rate limit buckets", removedCount);
        }
    }

    /**
     * Gets the current number of buckets in memory.
     */
    public int getBucketCount() {
        return buckets.size();
    }
}
