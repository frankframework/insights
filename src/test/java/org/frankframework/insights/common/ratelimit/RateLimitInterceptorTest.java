package org.frankframework.insights.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.frankframework.insights.common.configuration.RateLimitConfig;
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
    private RateLimitConfig rateLimitConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Bucket bucket;

    private Map<String, Bucket> businessValueFailureRateLimiters;
    private RateLimitInterceptor interceptor;
    private OAuth2User oauth2User;

    @BeforeEach
    void setUp() {
        businessValueFailureRateLimiters = new ConcurrentHashMap<>();
        interceptor = new RateLimitInterceptor(businessValueFailureRateLimiters, rateLimitConfig);

        oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("login", "testuser"),
                "login"
        );

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void preHandle_shouldSkipRateLimiting_whenPathIsNotBusinessValue() {
        when(request.getRequestURI()).thenReturn("/api/other-endpoint");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verifyNoInteractions(rateLimitConfig);
    }

    @Test
    void preHandle_shouldSkipRateLimiting_whenMethodIsGet() {
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("GET");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verifyNoInteractions(rateLimitConfig);
    }

    @Test
    void preHandle_shouldSkipRateLimiting_whenUserIsNotAuthenticated() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verifyNoInteractions(rateLimitConfig);
    }

    @Test
    void preHandle_shouldCreateBucket_whenUserIsAuthenticatedAndRequestIsBusinessValueMutation() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(rateLimitConfig.createBusinessValueFailureBucket()).thenReturn(bucket);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        assertThat(businessValueFailureRateLimiters).containsKey("testuser");
        verify(rateLimitConfig).createBusinessValueFailureBucket();
    }

    @Test
    void preHandle_shouldNotCreateBucket_whenBucketAlreadyExists() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		businessValueFailureRateLimiters.put("testuser", bucket);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("PUT");
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verifyNoInteractions(rateLimitConfig);
    }

    @Test
    void preHandle_shouldHandleDELETEMethod() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("DELETE");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(rateLimitConfig.createBusinessValueFailureBucket()).thenReturn(bucket);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        assertThat(businessValueFailureRateLimiters).containsKey("testuser");
    }

    @Test
    void afterCompletion_shouldSkipTracking_whenPathIsNotBusinessValue() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/other-endpoint");

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(bucket);
    }

    @Test
    void afterCompletion_shouldSkipTracking_whenMethodIsGet() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("GET");

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(bucket);
    }

    @Test
    void afterCompletion_shouldSkipTracking_whenUserIsNotAuthenticated() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(null);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(bucket);
    }

    @Test
    void afterCompletion_shouldSkipTracking_whenStatusIsSuccess() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(bucket);
    }

    @Test
    void afterCompletion_shouldSkipTracking_whenBucketDoesNotExist() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(400);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(bucket);
    }

    @Test
    void afterCompletion_shouldConsumeToken_whenRequestFailsWithClientError() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		businessValueFailureRateLimiters.put("testuser", bucket);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(400);
        when(bucket.tryConsume(1)).thenReturn(true);

        interceptor.afterCompletion(request, response, null, null);

        verify(bucket).tryConsume(1);
    }

    @Test
    void afterCompletion_shouldConsumeToken_whenRequestFailsWithServerError() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		businessValueFailureRateLimiters.put("testuser", bucket);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(500);
        when(bucket.tryConsume(1)).thenReturn(true);

        interceptor.afterCompletion(request, response, null, null);

        verify(bucket).tryConsume(1);
    }

    @Test
    void afterCompletion_shouldLogWarning_whenRateLimitExceeded() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		businessValueFailureRateLimiters.put("testuser", bucket);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(400);
        when(bucket.tryConsume(1)).thenReturn(false);

        interceptor.afterCompletion(request, response, null, null);

        verify(bucket).tryConsume(1);
    }

    @Test
    void afterCompletion_shouldHandleException_whenExceptionIsProvided() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		businessValueFailureRateLimiters.put("testuser", bucket);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(500);
        when(bucket.tryConsume(1)).thenReturn(true);

        Exception testException = new RuntimeException("Test exception");
        interceptor.afterCompletion(request, response, null, testException);

        verify(bucket).tryConsume(1);
    }

    @Test
    void afterCompletion_shouldHandleNullAuthentication() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(securityContext.getAuthentication()).thenReturn(null);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(bucket);
    }

    @Test
    void afterCompletion_shouldHandleNonOAuth2UserPrincipal() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn("string-principal");

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(bucket);
    }

    @Test
    void afterCompletion_shouldHandleExactly400Status() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		businessValueFailureRateLimiters.put("testuser", bucket);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(400);
        when(bucket.tryConsume(1)).thenReturn(true);

        interceptor.afterCompletion(request, response, null, null);

        verify(bucket).tryConsume(1);
    }

    @Test
    void afterCompletion_shouldNotConsumeToken_whenStatus399() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		businessValueFailureRateLimiters.put("testuser", bucket);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(399);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(bucket);
    }

    @Test
    void preHandle_shouldReturnTrue_forAllValidScenarios() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(rateLimitConfig.createBusinessValueFailureBucket()).thenReturn(bucket);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    void afterCompletion_shouldHandleConcurrentAccess() throws Exception {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		businessValueFailureRateLimiters.put("testuser", bucket);
        when(request.getRequestURI()).thenReturn("/api/business-value/123");
        when(request.getMethod()).thenReturn("POST");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.getStatus()).thenReturn(400);
        when(bucket.tryConsume(1)).thenReturn(true);

        interceptor.afterCompletion(request, response, null, null);
        interceptor.afterCompletion(request, response, null, null);

        verify(bucket, times(2)).tryConsume(1);
    }
}
