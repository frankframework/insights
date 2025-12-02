package org.frankframework.insights.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import org.frankframework.insights.ratelimit.RateLimitExceededException;
import org.frankframework.insights.ratelimit.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
public class RateLimitInterceptorTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private RateLimitInterceptor interceptor;
    private OAuth2User oauth2User;

    @BeforeEach
	public void setUp() {
        interceptor = new RateLimitInterceptor(rateLimitService);

        oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("login", "testuser"),
                "login"
        );

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
	public void preHandle_shouldSkipRateLimiting_forBusinessValueReleaseEndpoint() throws RateLimitExceededException {
        when(request.getRequestURI()).thenReturn("/api/business-value/release/123");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verifyNoInteractions(rateLimitService);
    }

    @Test
	public void preHandle_shouldSkipRateLimiting_forNonProtectedEndpoints() throws RateLimitExceededException {
        when(request.getRequestURI()).thenReturn("/api/releases");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verifyNoInteractions(rateLimitService);
    }

    @Test
	public void preHandle_shouldSkipRateLimiting_whenUserIsNotAuthenticated() throws RateLimitExceededException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(authentication.getPrincipal()).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verifyNoInteractions(rateLimitService);
    }

    @Test
	public void preHandle_shouldCheckIfBlocked_forAuthEndpoint() throws RateLimitExceededException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/auth/user");
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verify(rateLimitService).checkIfBlocked("testuser");
    }

    @Test
	public void preHandle_shouldCheckIfBlocked_forBusinessValueEndpoint() throws RateLimitExceededException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verify(rateLimitService).checkIfBlocked("testuser");
    }

    @Test
	public void preHandle_shouldThrowException_whenUserIsBlocked() throws RateLimitExceededException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        doThrow(new RateLimitExceededException("Too many failed requests"))
                .when(rateLimitService).checkIfBlocked("testuser");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many failed requests");
    }

    @Test
	public void afterCompletion_shouldTrackFailedRequest_when400Status() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(400);

        interceptor.afterCompletion(request, response, null, null);

        verify(rateLimitService).trackFailedRequest("testuser");
        verify(rateLimitService, never()).resetRateLimit(anyString());
    }

    @Test
	public void afterCompletion_shouldTrackFailedRequest_when404Status() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("GET");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(404);

        interceptor.afterCompletion(request, response, null, null);

        verify(rateLimitService).trackFailedRequest("testuser");
        verify(rateLimitService, never()).resetRateLimit(anyString());
    }

    @Test
	public void afterCompletion_shouldTrackFailedRequest_when500Status() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/auth/user");
        when(request.getMethod()).thenReturn("GET");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(500);

        interceptor.afterCompletion(request, response, null, null);

        verify(rateLimitService).trackFailedRequest("testuser");
        verify(rateLimitService, never()).resetRateLimit(anyString());
    }

    @Test
	public void afterCompletion_shouldResetRateLimit_when200Status() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verify(rateLimitService).resetRateLimit("testuser");
        verify(rateLimitService, never()).trackFailedRequest(anyString());
    }

    @Test
	public void afterCompletion_shouldResetRateLimit_when201Status() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(201);

        interceptor.afterCompletion(request, response, null, null);

        verify(rateLimitService).resetRateLimit("testuser");
        verify(rateLimitService, never()).trackFailedRequest(anyString());
    }

    @Test
	public void afterCompletion_shouldResetRateLimit_when204Status() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("DELETE");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(204);

        interceptor.afterCompletion(request, response, null, null);

        verify(rateLimitService).resetRateLimit("testuser");
        verify(rateLimitService, never()).trackFailedRequest(anyString());
    }

    @Test
	public void afterCompletion_shouldSkipTracking_forBusinessValueReleaseEndpoint() {
        when(request.getRequestURI()).thenReturn("/api/business-value/release/123");

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(rateLimitService);
    }

    @Test
	public void afterCompletion_shouldSkipTracking_forNonProtectedEndpoints() {
        when(request.getRequestURI()).thenReturn("/api/releases");

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(rateLimitService);
    }

    @Test
	public void afterCompletion_shouldSkipTracking_whenUserIsNotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(authentication.getPrincipal()).thenReturn(null);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(rateLimitService);
    }

    @Test
	public void preHandle_shouldHandleNonOAuth2UserPrincipal() throws RateLimitExceededException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(authentication.getPrincipal()).thenReturn("string-principal");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verifyNoInteractions(rateLimitService);
    }

    @Test
	public void afterCompletion_shouldHandleNullAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/business-value");

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(rateLimitService);
    }

    @Test
	public void afterCompletion_shouldHandleException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(500);

        Exception testException = new RuntimeException("Test exception");
        interceptor.afterCompletion(request, response, null, testException);

        verify(rateLimitService).trackFailedRequest("testuser");
    }

    @Test
	public void afterCompletion_shouldHandleBoundaryStatus_399() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/business-value");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(399);

        interceptor.afterCompletion(request, response, null, null);

        verify(rateLimitService).resetRateLimit("testuser");
        verify(rateLimitService, never()).trackFailedRequest(anyString());
    }

    @Test
    public void afterCompletion_shouldHandleAllAuthEndpoints() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(200);

        when(request.getRequestURI()).thenReturn("/api/auth/user");
        interceptor.afterCompletion(request, response, null, null);
        verify(rateLimitService, times(1)).resetRateLimit("testuser");

        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        interceptor.afterCompletion(request, response, null, null);
        verify(rateLimitService, times(2)).resetRateLimit("testuser");
    }
}
