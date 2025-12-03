package org.frankframework.insights.common.configuration;

import static org.mockito.Mockito.mock;

import org.frankframework.insights.common.ratelimit.RateLimitInterceptor;
import org.frankframework.insights.ratelimit.RateLimitService;
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
    public RateLimitService rateLimitService() {
        return mock(RateLimitService.class);
    }

    @Bean
    public RateLimitInterceptor rateLimitInterceptor(RateLimitService rateLimitService) {
        return new RateLimitInterceptor(rateLimitService);
    }
}
