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
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class OAuth2LoginSuccessHandlerTest {

    private OAuth2LoginSuccessHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        handler = new OAuth2LoginSuccessHandler();
        when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    public void onAuthenticationSuccess_withRefererHeader_redirectsToOriginWithSuccessParam() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:4200/dashboard");
        when(authentication.getName()).thenReturn("testuser");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:4200/?login=success");
        verify(request).getHeader("Referer");
    }

    @Test
    public void onAuthenticationSuccess_withDifferentPort_redirectsToCorrectOrigin() throws IOException {
        when(request.getHeader("Referer")).thenReturn("http://localhost:8080/");
        when(authentication.getName()).thenReturn("testuser");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8080/?login=success");
    }

    @Test
    public void onAuthenticationSuccess_withProductionUrl_redirectsToProduction() throws IOException {
        when(request.getHeader("Referer")).thenReturn("https://insights.frankframework.org/");
        when(authentication.getName()).thenReturn("testuser");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("https://insights.frankframework.org/?login=success");
    }

    @Test
    public void onAuthenticationSuccess_withoutRefererHeader_redirectsToRootWithSuccessParam() throws IOException {
        when(request.getHeader("Referer")).thenReturn(null);
        when(authentication.getName()).thenReturn("testuser");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/?login=success");
    }

    @Test
    public void onAuthenticationSuccess_withEmptyRefererHeader_redirectsToRootWithSuccessParam() throws IOException {
        when(request.getHeader("Referer")).thenReturn("");
        when(authentication.getName()).thenReturn("testuser");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/?login=success");
    }

    @Test
    public void onAuthenticationSuccess_withInvalidReferer_redirectsToRootWithSuccessParam() throws IOException {
        when(request.getHeader("Referer")).thenReturn("not-a-valid-url");
        when(authentication.getName()).thenReturn("testuser");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/?login=success");
    }
}
