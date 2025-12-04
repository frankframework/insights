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
    public void onAuthenticationFailure_redirectsToRoot() throws IOException {
        when(exception.getMessage()).thenReturn("Authentication failed");

        handler.onAuthenticationFailure(request, response, exception);

        verify(response).sendRedirect("/");
    }
}
