package org.frankframework.insights.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.user.GitHubOAuthAttributes;
import org.frankframework.insights.user.User;
import org.frankframework.insights.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Custom OAuth2 login success handler that verifies FrankFramework organization membership
 * before allowing authentication to succeed. Only FrankFramework members can successfully
 * authenticate and access the application.
 */
@Component
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        log.info("OAuth2 authentication successful for user: {}", authentication.getName());

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            GitHubOAuthAttributes attributes = GitHubOAuthAttributes.from(oauth2User.getAttributes());

            User user = userRepository.findByGithubId(attributes.githubId()).orElse(null);

            if (user == null || !user.isFrankFrameworkMember()) {
                log.warn("User '{}' is not a FrankFramework member - denying access", attributes.username());
                request.getSession().invalidate();
                getRedirectStrategy().sendRedirect(request, response, "/?login=forbidden");
                return;
            }

            log.info("User '{}' is a verified FrankFramework member - granting access", attributes.username());
        }

        OAuth2RedirectUtil.redirectToOrigin(request, response, getRedirectStrategy(), "login=success");
    }
}
