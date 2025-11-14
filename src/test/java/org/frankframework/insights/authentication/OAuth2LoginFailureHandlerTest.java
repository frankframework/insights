package org.frankframework.insights.authentication;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
public class OAuth2LoginFailureHandlerTest {

    private OAuth2LoginFailureHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException exception;

    @BeforeEach
    public void setUp() {
        handler = new OAuth2LoginFailureHandler();
        when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    public void onAuthenticationFailure_withRefererHeader_redirectsToOriginWithErrorParam() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:4200/dashboard");
        when(exception.getMessage()).thenReturn("Authentication failed");

        handler.onAuthenticationFailure(request, response, exception);

        verify(response).sendRedirect("http://localhost:4200/?login=error");
        verify(request).getHeader("Referer");
    }

    @Test
    public void onAuthenticationFailure_withDifferentPort_redirectsToCorrectOrigin() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:8080/");
        when(exception.getMessage()).thenReturn("Authentication failed");

        handler.onAuthenticationFailure(request, response, exception);

        verify(response).sendRedirect("http://localhost:8080/?login=error");
    }

    @Test
    public void onAuthenticationFailure_withProductionUrl_redirectsToProduction() throws IOException {
        when(request.getHeader("Referer")).thenReturn("https://insights.frankframework.org/");
        when(exception.getMessage()).thenReturn("Authentication failed");

        handler.onAuthenticationFailure(request, response, exception);

        verify(response).sendRedirect("https://insights.frankframework.org/?login=error");
    }

    @Test
    public void onAuthenticationFailure_withoutRefererHeader_redirectsToRootWithErrorParam() throws IOException {
        when(request.getHeader("Referer")).thenReturn(null);
        when(exception.getMessage()).thenReturn("Authentication failed");

        handler.onAuthenticationFailure(request, response, exception);

        verify(response).sendRedirect("/?login=error");
    }

    @Test
    public void onAuthenticationFailure_withEmptyRefererHeader_redirectsToRootWithErrorParam() throws IOException {
        when(request.getHeader("Referer")).thenReturn("");
        when(exception.getMessage()).thenReturn("Authentication failed");

        handler.onAuthenticationFailure(request, response, exception);

        verify(response).sendRedirect("/?login=error");
    }

    @Test
    public void onAuthenticationFailure_withInvalidReferer_redirectsToRootWithErrorParam() throws IOException {
        when(request.getHeader("Referer")).thenReturn("not-a-valid-url");
        when(exception.getMessage()).thenReturn("Authentication failed");

        handler.onAuthenticationFailure(request, response, exception);

        verify(response).sendRedirect("/?login=error");
    }
}
