package org.frankframework.insights.authentication;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.frankframework.insights.user.User;
import org.frankframework.insights.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
public class OAuth2LoginSuccessHandlerTest {

    private OAuth2LoginSuccessHandler handler;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    @Mock
    private HttpSession session;

    @BeforeEach
    public void setUp() {
        handler = new OAuth2LoginSuccessHandler(userRepository);
        when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    public void onAuthenticationSuccess_withFrankFrameworkMember_redirectsToRoot() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345);
        attributes.put("login", "testuser");

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(attributes);

        User user = new User();
        user.setFrankFrameworkMember(true);
        when(userRepository.findByGithubId(12345L)).thenReturn(Optional.of(user));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/");
        verify(session, never()).invalidate();
    }

    @Test
    public void onAuthenticationSuccess_withNonFrankFrameworkMember_invalidatesSessionAndRedirects()
            throws IOException {
        when(request.getSession()).thenReturn(session);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345);
        attributes.put("login", "testuser");

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(attributes);

        User user = new User();
        user.setFrankFrameworkMember(false);
        when(userRepository.findByGithubId(12345L)).thenReturn(Optional.of(user));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(session).invalidate();
        verify(response).sendRedirect("/");
    }

    @Test
    public void onAuthenticationSuccess_withNonExistingUser_invalidatesSessionAndRedirects() throws IOException {
        when(request.getSession()).thenReturn(session);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345);
        attributes.put("login", "testuser");

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(attributes);
        when(userRepository.findByGithubId(12345L)).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(session).invalidate();
        verify(response).sendRedirect("/");
    }
}
