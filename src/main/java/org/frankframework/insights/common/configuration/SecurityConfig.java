package org.frankframework.insights.common.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.frankframework.insights.authentication.OAuth2LoginFailureHandler;
import org.frankframework.insights.authentication.OAuth2LoginSuccessHandler;
import org.frankframework.insights.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    private static final int MAX_ALLOWED_SESSIONS = 3;
    private final UserService userService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Value("${frankframework.security.csrf.secure:true}")
    private boolean csrfCookieSecure;

    public SecurityConfig(
            UserService userService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oAuth2LoginFailureHandler) {
        this.userService = userService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry)
            throws Exception {

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        return http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/business-value/release/**")
                        .permitAll()
                        .requestMatchers("/api/auth/user", "/api/business-value/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userService(userService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()).csrfTokenRequestHandler(requestHandler))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(buildCspDirectives()))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId)
                        .maximumSessions(MAX_ALLOWED_SESSIONS)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry))
                .build();
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();

        repository.setCookieCustomizer(cookie -> cookie.path("/").secure(csrfCookieSecure).httpOnly(false).sameSite("Lax"));

        return repository;
    }

    private String buildCspDirectives() {
        return String.join(
                "; ",
                "default-src 'self'",
                "script-src 'self' 'unsafe-inline'",
                "style-src 'self' 'unsafe-inline'",
                "img-src 'self' data: https:",
                "font-src 'self' data:",
                "connect-src 'self' https://api.github.com",
                "frame-ancestors 'none'",
                "base-uri 'self'",
                "form-action 'self'");
    }

    /**
     * Filter that forces the CSRF token to be generated and exposed.
     */
    private static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

            if (csrfToken != null) {
                response.setHeader("X-XSRF-TOKEN", csrfToken.getToken());
            }

            filterChain.doFilter(request, response);
        }
    }
}
