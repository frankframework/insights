package org.frankframework.insights.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import org.frankframework.insights.common.exception.ApiException;
import org.frankframework.insights.user.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationController authenticationController;

    private OAuth2User oauth2User;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    public void setUp() {
        Map<String, Object> attributes = Map.of(
                "id", 12345L,
                "login", "testuser",
                "avatar_url", "https://github.com/avatars/testuser.png");

        oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), attributes, "login");

        userResponseDTO = new UserResponseDTO(12345L, "testuser", "https://github.com/avatars/testuser.png", true);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void getCurrentUser_whenAuthenticatedAndMember_returnsUserInfo() throws ApiException {
        when(authenticationService.getUserInfo(oauth2User)).thenReturn(userResponseDTO);

        ResponseEntity<UserResponseDTO> result = authenticationController.getCurrentUser(oauth2User);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().githubId()).isEqualTo(12345L);
        assertThat(result.getBody().username()).isEqualTo("testuser");
        assertThat(result.getBody().isFrankFrameworkMember()).isTrue();

        verify(authenticationService).getUserInfo(oauth2User);
    }

    @Test
    public void getCurrentUser_whenNotAuthenticated_throwsUnauthorizedException() throws ApiException {
        when(authenticationService.getUserInfo(null))
                .thenThrow(new UnauthorizedException("You are not logged in. Please sign in with GitHub."));

        assertThatThrownBy(() -> authenticationController.getCurrentUser(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You are not logged in. Please sign in with GitHub.");

        verify(authenticationService).getUserInfo(null);
    }

    @Test
    public void getCurrentUser_whenNotMember_throwsForbiddenException() throws ApiException {
        when(authenticationService.getUserInfo(oauth2User))
                .thenThrow(new ForbiddenException(
                        "Access denied. You must be a member of the frankframework organization on GitHub."));

        assertThatThrownBy(() -> authenticationController.getCurrentUser(oauth2User))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied. You must be a member of the frankframework organization on GitHub.");

        verify(authenticationService).getUserInfo(oauth2User);
    }
}
