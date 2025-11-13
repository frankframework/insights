package org.frankframework.insights.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;
import java.net.URI;

/**
 * Utility class for OAuth2 handlers that provides common redirect logic.
 * Extracts the origin from the Referer header to redirect to the correct port.
 */
@Slf4j
public final class OAuth2RedirectUtil {

    private OAuth2RedirectUtil() {
        // Utility class
    }

    /**
     * Redirects to the origin of the initial request based on the Referer header.
     * Falls back to "/" if no referer is present or parsing fails.
     *
     * @param queryParam Optional query parameter to add (e.g., "login=success")
     */
    public static void redirectToOrigin(
            HttpServletRequest request, HttpServletResponse response, RedirectStrategy redirectStrategy, String queryParam)
            throws IOException {
        String referer = request.getHeader("Referer");
        String redirectUrl = "/";

        if (referer != null && !referer.isEmpty()) {
            try {
                URI uri = URI.create(referer);
                String scheme = uri.getScheme();
                String authority = uri.getAuthority();

                if (scheme != null && authority != null) {
                    redirectUrl = scheme + "://" + authority + "/";
                    log.info("Redirecting to origin: {}", redirectUrl);
                } else {
                    log.warn("Could not extract scheme or authority from referer: {}", referer);
                }
            } catch (Exception e) {
                log.warn("Could not parse referer header: {}, using default redirect", referer, e);
            }
        } else {
            log.info("No referer header present, redirecting to root");
        }

        // Add query parameter if provided
        if (queryParam != null && !queryParam.isEmpty()) {
            redirectUrl += "?" + queryParam;
        }

        redirectStrategy.sendRedirect(request, response, redirectUrl);
    }
}
