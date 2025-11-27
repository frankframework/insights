package org.frankframework.insights.common.ratelimit;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.configuration.RateLimitConfig;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int HTTP_BAD_REQUEST = 400;

    private final Map<String, Bucket> businessValueFailureRateLimiters;
    private final RateLimitConfig rateLimitConfig;
    private final Map<String, Integer> requestAttempts = new java.util.concurrent.ConcurrentHashMap<>();

    public RateLimitInterceptor(Map<String, Bucket> businessValueFailureRateLimiters, RateLimitConfig rateLimitConfig) {
        this.businessValueFailureRateLimiters = businessValueFailureRateLimiters;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();

        // Only apply rate limiting to business value endpoints
        if (!path.startsWith("/api/business-value")) {
            return true;
        }

        // Only rate limit POST, PUT, DELETE requests (state-changing operations)
        String method = request.getMethod();
        if (!method.equals("POST") && !method.equals("PUT") && !method.equals("DELETE")) {
            return true;
        }

        String userKey = getUserKey();
        if (userKey == null) {
            return true; // No rate limiting for unauthenticated requests (will be blocked by security anyway)
        }

        Bucket bucket = businessValueFailureRateLimiters.computeIfAbsent(
                userKey, k -> rateLimitConfig.createBusinessValueFailureBucket());

        // Store request attempt for afterCompletion
        requestAttempts.put(getRequestId(request), 0);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        String path = request.getRequestURI();

        // Only track business value endpoints
        if (!path.startsWith("/api/business-value")) {
            return;
        }

        String method = request.getMethod();
        if (!method.equals("POST") && !method.equals("PUT") && !method.equals("DELETE")) {
            return;
        }

        String userKey = getUserKey();
        if (userKey == null) {
            return;
        }

        int statusCode = response.getStatus();
        String requestId = getRequestId(request);

        // Only consume tokens for failed requests (4xx and 5xx errors)
        if (statusCode >= HTTP_BAD_REQUEST) {
            Bucket bucket = businessValueFailureRateLimiters.get(userKey);
            if (bucket != null && !bucket.tryConsume(1)) {
                log.warn(
                        "User {} has exceeded rate limit for failed business value requests. Status: {}",
                        userKey,
                        statusCode);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\": \"Too many failed requests. Please try again later.\"}");
            } else {
                log.debug("Failed request tracked for user {}: {} {}", userKey, method, path);
            }
        }

        // Clean up
        requestAttempts.remove(requestId);
    }

    private String getUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("login");
        }
        return null;
    }

    private String getRequestId(HttpServletRequest request) {
        return request.getRequestURI() + "-" + System.currentTimeMillis();
    }
}
