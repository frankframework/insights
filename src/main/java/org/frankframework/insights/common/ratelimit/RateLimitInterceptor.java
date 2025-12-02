package org.frankframework.insights.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.ratelimit.RateLimitExceededException;
import org.frankframework.insights.ratelimit.RateLimitService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String API_AUTH_PATH = "/api/auth";
    private static final String API_BUSINESS_VALUE_PATH = "/api/business-value";
    private static final String API_BUSINESS_VALUE_RELEASE_PATH = "/api/business-value/release";
    private static final String API_VULNERABILITIES_PATH = "/api/vulnerabilities";
    private static final String API_VULNERABILITIES_RELEASE_PATH = "/api/vulnerabilities/release";

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws RateLimitExceededException {
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith(API_BUSINESS_VALUE_RELEASE_PATH)
                || requestURI.startsWith(API_VULNERABILITIES_RELEASE_PATH)) {
            return true;
        }

        if (shouldSkipRateLimiting(requestURI)) {
            return true;
        }

        String userKey = getUserKey();
        if (userKey == null) {
            log.debug("No authenticated user found, skipping rate limit");
            return true;
        }

        rateLimitService.checkIfBlocked(userKey);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith(API_BUSINESS_VALUE_RELEASE_PATH)
                || requestURI.startsWith(API_VULNERABILITIES_RELEASE_PATH)) {
            return;
        }

        if (shouldSkipRateLimiting(requestURI)) {
            return;
        }

        String userKey = getUserKey();
        if (userKey == null) {
            return;
        }

        int statusCode = response.getStatus();

        if (statusCode >= HttpStatus.BAD_REQUEST.value()) {
            rateLimitService.trackFailedRequest(userKey);
            log.debug(
                    "Tracked failed request for user {}: {} {} (status {})",
                    userKey,
                    request.getMethod(),
                    requestURI,
                    statusCode);
        } else {
            rateLimitService.resetRateLimit(userKey);
            log.debug(
                    "Reset rate limit for user {} after successful request: {} {} (status {})",
                    userKey,
                    request.getMethod(),
                    requestURI,
                    statusCode);
        }
    }

    /**
     * Checks if rate limiting should be skipped for this request URI.
     */
    private boolean shouldSkipRateLimiting(String requestURI) {
        return !requestURI.startsWith(API_AUTH_PATH)
                && !requestURI.startsWith(API_BUSINESS_VALUE_PATH)
                && !requestURI.startsWith(API_VULNERABILITIES_PATH);
    }

    /**
     * Extracts the GitHub login from the authenticated OAuth2User.
     */
    private String getUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("login");
        }
        return null;
    }
}
