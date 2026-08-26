package org.frankframework.insights.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
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

    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws RateLimitExceededException {
        String requestURI = request.getRequestURI();

        if (!isRateLimited(requestURI)) {
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

        if (!isRateLimited(requestURI)) {
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
     * Checks if rate limiting applies to this request URI.
     */
    private boolean isRateLimited(String requestURI) {
        return startsWithAny(requestURI, rateLimitProperties.getProtectedPaths())
                && !startsWithAny(requestURI, rateLimitProperties.getExemptPaths());
    }

    private boolean startsWithAny(String requestURI, List<String> paths) {
        return paths != null && paths.stream().anyMatch(requestURI::startsWith);
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
