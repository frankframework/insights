package org.frankframework.insights.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Custom OAuth2 login failure handler that redirects to the frontend
 * with a query parameter indicating failed authentication.
 */
@Component
@Slf4j
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage());
        getRedirectStrategy().sendRedirect(request, response, "/?login=error");
    }
}
