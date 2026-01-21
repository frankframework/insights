package org.frankframework.insights.common.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.frankframework.insights.authentication.OAuth2LoginFailureHandler;
import org.frankframework.insights.authentication.OAuth2LoginSuccessHandler;
import org.frankframework.insights.user.UserRepository;
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
    private final UserRepository userRepository;

    @Value("${frankframework.security.csrf.secure:true}")
    private boolean csrfCookieSecure;

    public SecurityConfig(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
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
                        .requestMatchers("/api/business-value/release/**", "/api/vulnerabilities/release/**")
                        .permitAll()
                        .requestMatchers("/api/auth/user", "/api/business-value/**", "/api/vulnerabilities/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userService(userService))
                        .successHandler(new OAuth2LoginSuccessHandler(userRepository))
                        .failureHandler(new OAuth2LoginFailureHandler()))
                .exceptionHandling(
                        exceptions -> exceptions.authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().flush();
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "SECURE-XSRF-TOKEN", "XSRF-TOKEN")
                        .permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()).csrfTokenRequestHandler(requestHandler))
                .addFilterAfter(new CsrfCookieFilter(csrfCookieSecure), CsrfFilter.class)
                .headers(headers -> headers.contentSecurityPolicy(csp ->
                                csp.policyDirectives(buildCspDirectives()).reportOnly())
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

        repository.setCookieName("SECURE-XSRF-TOKEN");

        repository.setCookieCustomizer(cookie ->
                cookie.path("/").secure(csrfCookieSecure).httpOnly(true).sameSite("Lax"));

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

    private static class CsrfCookieFilter extends OncePerRequestFilter {
        private final boolean isSecure;

        public CsrfCookieFilter(boolean isSecure) {
            this.isSecure = isSecure;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

            if (csrfToken != null) {
                response.setHeader("X-XSRF-TOKEN", csrfToken.getToken());

                Cookie cookie = new Cookie("XSRF-TOKEN", csrfToken.getToken());
                cookie.setPath("/");
                cookie.setSecure(isSecure);
                cookie.setHttpOnly(false);
                response.addCookie(cookie);
            }

            filterChain.doFilter(request, response);
        }
    }
}
