package org.frankframework.insights.common.configuration;

import static org.mockito.Mockito.mock;

import io.github.bucket4j.Bucket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.frankframework.insights.common.ratelimit.RateLimitInterceptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that permits all requests without authentication.
 * Used by controller tests to bypass security requirements.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public RateLimitConfig rateLimitConfig() {
        return new RateLimitConfig();
    }

    @Bean
    public Map<String, Bucket> businessValueFailureRateLimiters() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public RateLimitInterceptor rateLimitInterceptor(
            Map<String, Bucket> businessValueFailureRateLimiters, RateLimitConfig rateLimitConfig) {
        return new RateLimitInterceptor(businessValueFailureRateLimiters, rateLimitConfig);
    }
}
