package org.frankframework.insights.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import org.frankframework.insights.github.rest.GitHubRestClient;
import org.frankframework.insights.github.rest.GitHubRestClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Comprehensive integration tests for UserService that test the full OAuth2 authentication flow
 * with real H2 database interactions and mocked external GitHub API calls.
 *
 * These tests verify:
 * - OAuth2 user loading and authentication
 * - Database persistence and updates
 * - Organization membership verification
 * - Concurrent user operations
 * - Error handling and recovery
 * - Edge cases and boundary conditions
 */
@SpringBootTest
@ActiveProfiles("local-seed")
public class UserServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @MockitoBean
    private GitHubRestClient gitHubRestClient;

    private UserService userService;
    private ClientRegistration clientRegistration;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();

        if (mockServer != null) {
            mockServer.reset();
        }

        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        userService = new UserService(userRepository, userMapper, gitHubRestClient);
        userService.setRestOperations(restTemplate);

        clientRegistration = ClientRegistration.withRegistrationId("github")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("test-client-id")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
        if (mockServer != null) {
            mockServer.reset();
        }
    }

    @Test
    public void loadUser_newUserWithMembership_shouldCreateUserInDatabase() throws GitHubRestClientException {
        mockGitHubUserEndpoint(12345L, "testuser", "https://github.com/avatars/testuser");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("test-access-token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership("test-access-token", "testuser"))
                .thenReturn(true);

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).containsKey("id");
        assertThat(result.getAttributes()).containsEntry("login", "testuser");

        Optional<User> savedUser = userRepository.findByGithubId(12345L);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEqualTo("testuser");
        assertThat(savedUser.get().isFrankFrameworkMember()).isTrue();
        assertThat(savedUser.get().getCreatedAt()).isNotNull();
        assertThat(savedUser.get().getUpdatedAt()).isNotNull();

        verify(gitHubRestClient).checkOAuthUserOrganizationMembership("test-access-token", "testuser");
    }

    @Test
    public void loadUser_newUserWithoutMembership_shouldCreateUserWithMembershipFalse()
            throws GitHubRestClientException {
        mockGitHubUserEndpoint(99999L, "nonmember", "https://github.com/avatars/nonmember");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("test-access-token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership("test-access-token", "nonmember"))
                .thenReturn(false);

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();

        Optional<User> savedUser = userRepository.findByGithubId(99999L);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEqualTo("nonmember");
        assertThat(savedUser.get().isFrankFrameworkMember()).isFalse();
    }

    @Test
    public void loadUser_existingUserWithMembershipChange_shouldUpdateMembershipStatus()
            throws GitHubRestClientException {
        User existingUser = User.builder()
                .githubId(55555L)
                .username("updateduser")
                .avatarUrl("https://github.com/avatars/old.png")
                .isFrankFrameworkMember(false)
                .build();
        existingUser = userRepository.save(existingUser);
        OffsetDateTime originalCreatedAt = existingUser.getCreatedAt();
        UUID originalId = existingUser.getId();

        mockGitHubUserEndpoint(55555L, "updateduser", "https://github.com/avatars/new.png");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("test-access-token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership("test-access-token", "updateduser"))
                .thenReturn(true);

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();

        Optional<User> updatedUser = userRepository.findByGithubId(55555L);
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getId()).isEqualTo(originalId);
        assertThat(updatedUser.get().getUsername()).isEqualTo("updateduser");
        assertThat(updatedUser.get().getAvatarUrl()).isEqualTo("https://github.com/avatars/new.png");
        assertThat(updatedUser.get().isFrankFrameworkMember()).isTrue();
        assertThat(updatedUser.get().getCreatedAt().truncatedTo(ChronoUnit.SECONDS))
                .isEqualTo(originalCreatedAt.truncatedTo(ChronoUnit.SECONDS));
        assertThat(updatedUser.get().getUpdatedAt()).isAfter(originalCreatedAt);

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    public void loadUser_existingUserWithUsernameChange_shouldUpdateUsername() throws GitHubRestClientException {
        User existingUser = User.builder()
                .githubId(77777L)
                .username("oldusername")
                .avatarUrl("https://github.com/avatars/user.png")
                .isFrankFrameworkMember(true)
                .build();
        userRepository.save(existingUser);

        mockGitHubUserEndpoint(77777L, "newusername", "https://github.com/avatars/user.png");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("test-access-token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership("test-access-token", "newusername"))
                .thenReturn(true);

        userService.loadUser(userRequest);

        Optional<User> updatedUser = userRepository.findByGithubId(77777L);
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getUsername()).isEqualTo("newusername");

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    public void loadUser_membershipCheckFails_shouldMarkUserAsNonMember() throws GitHubRestClientException {
        mockGitHubUserEndpoint(11111L, "erroruser", "https://github.com/avatars/erroruser");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("test-access-token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership("test-access-token", "erroruser"))
                .thenThrow(new GitHubRestClientException("GitHub API error", new RuntimeException()));

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();

        Optional<User> savedUser = userRepository.findByGithubId(11111L);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().isFrankFrameworkMember()).isFalse();
    }

    @Test
    public void loadUser_nullAccessToken_shouldHandleGracefully() throws GitHubRestClientException {
        mockGitHubUserEndpoint(22222L, "nulltokenuser", "https://github.com/avatars/nulltokenuser");
        OAuth2UserRequest userRequest = createOAuth2UserRequest(null);
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(isNull(), eq("nulltokenuser")))
                .thenThrow(
                        new GitHubRestClientException("Access token cannot be null", new IllegalArgumentException()));

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();

        Optional<User> savedUser = userRepository.findByGithubId(22222L);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().isFrankFrameworkMember()).isFalse();
    }

    @Test
    public void loadUser_invalidOAuth2Attributes_shouldThrowException() {
        String invalidJsonResponse = "{\"invalid_key\": \"invalid_value\"}";

        mockServer
                .expect(requestTo("https://api.github.com/user"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(invalidJsonResponse, MediaType.APPLICATION_JSON));

        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-token", null, null);

        OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, token);

        assertThatThrownBy(() -> userService.loadUser(userRequest)).isInstanceOf(Exception.class);
    }

    @Test
    public void loadUser_multipleUsers_shouldMaintainDataIntegrity() throws GitHubRestClientException {
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), anyString()))
                .thenReturn(true);

        mockGitHubUserEndpoint(1001L, "user1", "https://github.com/avatars/user1");
        mockGitHubUserEndpoint(1002L, "user2", "https://github.com/avatars/user2");
        mockGitHubUserEndpoint(1003L, "user3", "https://github.com/avatars/user3");

        OAuth2UserRequest user1Request = createOAuth2UserRequest("token1");
        OAuth2UserRequest user2Request = createOAuth2UserRequest("token2");
        OAuth2UserRequest user3Request = createOAuth2UserRequest("token3");

        userService.loadUser(user1Request);
        userService.loadUser(user2Request);
        userService.loadUser(user3Request);

        List<User> allUsers = userRepository.findAll();
        assertThat(allUsers).hasSize(3);
        assertThat(allUsers).extracting("username").containsExactlyInAnyOrder("user1", "user2", "user3");
        assertThat(allUsers).extracting("githubId").containsExactlyInAnyOrder(1001L, 1002L, 1003L);
        assertThat(allUsers).allMatch(User::isFrankFrameworkMember);
    }

    @Test
    public void loadUser_sameUserMultipleTimes_shouldUpdateNotDuplicate() throws GitHubRestClientException {
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), eq("repeatuser")))
                .thenReturn(true);

        mockGitHubUserEndpoint(2001L, "repeatuser", "https://github.com/avatars/repeatuser", 5);
        OAuth2UserRequest userRequest = createOAuth2UserRequest("token");

        for (int i = 0; i < 5; i++) {
            userService.loadUser(userRequest);
        }

        List<User> allUsers = userRepository.findAll();
        assertThat(allUsers).hasSize(1);
        assertThat(allUsers.getFirst().getUsername()).isEqualTo("repeatuser");
        assertThat(allUsers.getFirst().getGithubId()).isEqualTo(2001L);
    }

    @Test
    public void loadUser_concurrentAccess_shouldHandleCorrectly() throws Exception {
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), eq("concurrentuser")))
                .thenReturn(true);

        mockGitHubUserEndpoint(3001L, "concurrentuser", "https://github.com/avatars/concurrentuser", 10);
        OAuth2UserRequest userRequest = createOAuth2UserRequest("token");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        List<Future<OAuth2User>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    return userService.loadUser(userRequest);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<OAuth2User> future : futures) {
            OAuth2User result = future.get(5, TimeUnit.SECONDS);
            assertThat(result).isNotNull();
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        List<User> allUsers = userRepository.findAll();
        assertThat(allUsers).hasSize(1);
        assertThat(allUsers.getFirst().getUsername()).isEqualTo("concurrentuser");
        assertThat(allUsers.getFirst().getGithubId()).isEqualTo(3001L);
    }

    @Test
    public void loadUser_databaseConstraints_shouldEnforceUniqueGithubId() throws GitHubRestClientException {
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), anyString()))
                .thenReturn(true);

        mockGitHubUserEndpoint(4001L, "user1", "https://github.com/avatars/user1");
        mockGitHubUserEndpoint(4001L, "user2", "https://github.com/avatars/user2");

        OAuth2UserRequest user1Request = createOAuth2UserRequest("token1");
        userService.loadUser(user1Request);

        OAuth2UserRequest user2Request = createOAuth2UserRequest("token2");
        userService.loadUser(user2Request);

        List<User> allUsers = userRepository.findAll();
        assertThat(allUsers).hasSize(1);
        assertThat(allUsers.getFirst().getGithubId()).isEqualTo(4001L);
        assertThat(allUsers.getFirst().getUsername()).isEqualTo("user2");
    }

    @Test
    public void loadUser_emptyStringUsername_shouldSaveSuccessfully() throws GitHubRestClientException {
        mockGitHubUserEndpoint(5001L, "", "https://github.com/avatars/default");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), eq("")))
                .thenReturn(true);

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();
        Optional<User> savedUser = userRepository.findByGithubId(5001L);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEmpty();
    }

    @Test
    public void loadUser_nullAvatarUrl_shouldHandleGracefully() throws GitHubRestClientException {
        mockGitHubUserEndpoint(6001L, "noavatar", null);
        OAuth2UserRequest userRequest = createOAuth2UserRequest("token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), eq("noavatar")))
                .thenReturn(true);

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();
        Optional<User> savedUser = userRepository.findByGithubId(6001L);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getAvatarUrl()).isNull();
    }

    @Test
    public void loadUser_veryLongUsername_shouldHandleCorrectly() throws GitHubRestClientException {
        String longUsername = "a".repeat(39);
        mockGitHubUserEndpoint(7001L, longUsername, "https://github.com/avatars/avatar");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), eq(longUsername)))
                .thenReturn(true);

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();
        Optional<User> savedUser = userRepository.findByGithubId(7001L);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).hasSize(39);
    }

    @Test
    public void loadUser_specialCharactersInUsername_shouldHandleCorrectly() throws GitHubRestClientException {
        String specialUsername = "user-with-hyphens-123";
        mockGitHubUserEndpoint(8001L, specialUsername, "https://github.com/avatars/avatar");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), eq(specialUsername)))
                .thenReturn(true);

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();
        Optional<User> savedUser = userRepository.findByGithubId(8001L);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEqualTo(specialUsername);
    }

    @Test
    public void loadUser_maximumGithubId_shouldHandleCorrectly() throws GitHubRestClientException {
        Long maxGithubId = Long.MAX_VALUE;
        mockGitHubUserEndpoint(maxGithubId, "maxiduser", "https://github.com/avatars/avatar");
        OAuth2UserRequest userRequest = createOAuth2UserRequest("token");
        when(gitHubRestClient.checkOAuthUserOrganizationMembership(anyString(), eq("maxiduser")))
                .thenReturn(true);

        OAuth2User result = userService.loadUser(userRequest);

        assertThat(result).isNotNull();
        Optional<User> savedUser = userRepository.findByGithubId(maxGithubId);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getGithubId()).isEqualTo(maxGithubId);
    }

    /**
     * Sets up mock expectations for GitHub user info endpoint
     */
    private void mockGitHubUserEndpoint(Long githubId, String username, String avatarUrl, int times) {
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("id", githubId);
        userAttributes.put("login", username);
        if (avatarUrl != null) {
            userAttributes.put("avatar_url", avatarUrl);
        }

        try {
            String jsonResponse = new ObjectMapper().writeValueAsString(userAttributes);
            var expectation = mockServer
                    .expect(
                            org.springframework.test.web.client.ExpectedCount.times(times),
                            requestTo("https://api.github.com/user"))
                    .andExpect(method(HttpMethod.GET));

            expectation.andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock GitHub API response", e);
        }
    }

    private void mockGitHubUserEndpoint(Long githubId, String username, String avatarUrl) {
        mockGitHubUserEndpoint(githubId, username, avatarUrl, 1);
    }

    /**
     * Creates an OAuth2UserRequest with the specified parameters
     */
    private OAuth2UserRequest createOAuth2UserRequest(String accessToken) {
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, accessToken != null ? accessToken : "default-token", null, null);

        return new OAuth2UserRequest(clientRegistration, token);
    }
}
