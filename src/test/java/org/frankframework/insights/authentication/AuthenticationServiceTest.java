package org.frankframework.insights.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;
import org.frankframework.insights.common.exception.ApiException;
import org.frankframework.insights.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private AuthenticationService authenticationService;

    private Map<String, Object> oauthAttributes;
    private User testUser;

    @BeforeEach
    public void setUp() {
        oauthAttributes = Map.of(
                "id", 12345L,
                "login", "testuser",
                "avatar_url", "https://github.com/avatars/testuser.png");

        testUser = User.builder()
                .githubId(12345L)
                .username("testuser")
                .avatarUrl("https://github.com/avatars/testuser.png")
                .isFrankFrameworkMember(true)
                .build();
    }

    @Test
    public void getUserInfo_whenNotAuthenticated_throwsUnauthorizedException() {
        assertThatThrownBy(() -> authenticationService.getUserInfo(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You are not logged in. Please sign in with GitHub.");

        verify(userRepository, never()).findByGithubId(any());
        verify(userMapper, never()).toResponseDTO(any(), any());
    }

    @Test
    public void getUserInfo_whenAuthenticatedButNotFrankFrameworkMember_throwsForbiddenException() {
        when(oAuth2User.getAttributes()).thenReturn(oauthAttributes);
        when(userRepository.findByGithubId(12345L)).thenReturn(Optional.of(testUser));

        UserResponseDTO nonMemberResponse =
                new UserResponseDTO(12345L, "testuser", "https://github.com/avatars/testuser.png", false);
        when(userMapper.toResponseDTO(any(GitHubOAuthAttributes.class), eq(testUser)))
                .thenReturn(nonMemberResponse);

        assertThatThrownBy(() -> authenticationService.getUserInfo(oAuth2User))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied. You must be a member of the frankframework organization on GitHub.");

        verify(userRepository).findByGithubId(12345L);
        verify(userMapper).toResponseDTO(any(GitHubOAuthAttributes.class), eq(testUser));
    }

    @Test
    public void getUserInfo_whenAuthenticatedAndFrankFrameworkMember_returnsUserInfo() throws ApiException {
        when(oAuth2User.getAttributes()).thenReturn(oauthAttributes);
        when(userRepository.findByGithubId(12345L)).thenReturn(Optional.of(testUser));

        UserResponseDTO memberResponse =
                new UserResponseDTO(12345L, "testuser", "https://github.com/avatars/testuser.png", true);
        when(userMapper.toResponseDTO(any(GitHubOAuthAttributes.class), eq(testUser)))
                .thenReturn(memberResponse);

        UserResponseDTO result = authenticationService.getUserInfo(oAuth2User);

        assertThat(result).isNotNull();
        assertThat(result.githubId()).isEqualTo(12345L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.avatarUrl()).isEqualTo("https://github.com/avatars/testuser.png");
        assertThat(result.isFrankFrameworkMember()).isTrue();

        verify(userRepository).findByGithubId(12345L);
        verify(userMapper).toResponseDTO(any(GitHubOAuthAttributes.class), eq(testUser));
    }

    @Test
    public void getUserInfo_whenAuthenticatedButUserNotInDatabase_usesAttributesOnly() throws ApiException {
        when(oAuth2User.getAttributes()).thenReturn(oauthAttributes);
        when(userRepository.findByGithubId(12345L)).thenReturn(Optional.empty());

        UserResponseDTO memberResponse =
                new UserResponseDTO(12345L, "testuser", "https://github.com/avatars/testuser.png", true);
        when(userMapper.toResponseDTO(any(GitHubOAuthAttributes.class), isNull()))
                .thenReturn(memberResponse);

        UserResponseDTO result = authenticationService.getUserInfo(oAuth2User);

        assertThat(result).isNotNull();
        assertThat(result.githubId()).isEqualTo(12345L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.isFrankFrameworkMember()).isTrue();

        verify(userRepository).findByGithubId(12345L);
        verify(userMapper).toResponseDTO(any(GitHubOAuthAttributes.class), isNull());
    }
}
