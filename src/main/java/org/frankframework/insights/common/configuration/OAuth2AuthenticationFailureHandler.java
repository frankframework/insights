package org.frankframework.insights.common.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Custom handler for OAuth2 authentication failures
 * Provides specific error messages based on the failure reason
 */
@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {

		String errorMessage = "Authentication failed. Please try again.";

		if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
			String errorCode = oauth2Exception.getError().getErrorCode();
			String errorDescription = oauth2Exception.getError().getDescription();

			log.warn("OAuth2 authentication failed: {} - {}", errorCode, errorDescription);

			if ("access_denied".equals(errorCode) && errorDescription != null
					&& errorDescription.contains("frankframework organization")) {
				errorMessage =
						"Access denied. You must be a member of the frankframework organization on GitHub to access this application.";
			}
		} else {
			log.warn("Authentication failed: {}", exception.getMessage());
		}

		String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);

		getRedirectStrategy().sendRedirect(request, response, "/api/auth/failure?error=" + encodedError);
	}
}
