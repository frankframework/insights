package org.frankframework.insights.common.configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    private static final int MAX_FAILED_REQUESTS = 10;
    private static final int REFILL_TOKENS = 10;
    private static final int REFILL_DURATION_MINUTES = 5;

    /**
     * Rate limiter for failed business value requests - 10 failed requests per 5 minutes per user
     * This prevents abuse while allowing legitimate high-volume usage
     */
    @Bean
    public Map<String, Bucket> businessValueFailureRateLimiters() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Create a bucket for tracking failed business value operations
     * Allows 10 failed requests per 5 minutes, then blocks further requests
     */
    public Bucket createBusinessValueFailureBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_FAILED_REQUESTS)
                .refillIntervally(REFILL_TOKENS, Duration.ofMinutes(REFILL_DURATION_MINUTES))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
