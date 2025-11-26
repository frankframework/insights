package org.frankframework.insights.common.configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

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
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(5))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
