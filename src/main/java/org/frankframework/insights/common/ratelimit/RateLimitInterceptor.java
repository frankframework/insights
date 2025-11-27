package org.frankframework.insights.common.ratelimit;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.configuration.RateLimitConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String API_BUSINESS_VALUE_PATH = "/api/business-value";
    private static final int HTTP_BAD_REQUEST = 400;

    private final Map<String, Bucket> businessValueFailureRateLimiters;
    private final RateLimitConfig rateLimitConfig;

    public RateLimitInterceptor(Map<String, Bucket> businessValueFailureRateLimiters, RateLimitConfig rateLimitConfig) {
        this.businessValueFailureRateLimiters = businessValueFailureRateLimiters;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldSkipRateLimiting(request)) {
            return true;
        }

        String userKey = getUserKey();
        if (userKey == null) {
            return true;
        }

        businessValueFailureRateLimiters.computeIfAbsent(
                userKey, k -> rateLimitConfig.createBusinessValueFailureBucket());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (shouldSkipRateLimiting(request)) {
            return;
        }

        String userKey = getUserKey();
        if (userKey == null) {
            return;
        }

        int statusCode = response.getStatus();
        if (statusCode >= HTTP_BAD_REQUEST) {
            Bucket bucket = businessValueFailureRateLimiters.get(userKey);
            if (bucket == null) {
                return;
            }

            if (bucket.tryConsume(1)) {
                log.debug("Failed request tracked for user {}: {} {}", userKey, request.getMethod(), request.getRequestURI());
            } else {
                log.warn("User {} has exceeded rate limit for failed business value requests. Status: {}", userKey, statusCode);
            }
        }
    }

    private boolean shouldSkipRateLimiting(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith(API_BUSINESS_VALUE_PATH)) {
            return true;
        }

        String method = request.getMethod();
        return !"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method);
    }

    private String getUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("login");
        }
        return null;
    }
}
