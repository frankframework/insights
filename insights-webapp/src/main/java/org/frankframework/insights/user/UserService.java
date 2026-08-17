package org.frankframework.insights.user;

import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.github.rest.GitHubRestClient;
import org.frankframework.insights.github.rest.GitHubRestClientException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Service responsible for loading and managing OAuth2 users during the authentication flow.
 * Extends Spring Security's DefaultOAuth2UserService to customize user loading behavior.
 */
@Service
@Slf4j
public class UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GitHubRestClient gitHubRestClient;

    public UserService(UserRepository userRepository, UserMapper userMapper, GitHubRestClient gitHubRestClient) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.gitHubRestClient = gitHubRestClient;
    }

    /**
     * Loads user details from GitHub OAuth2 provider and checks organization membership.
     * This method:
     * 1. Loads OAuth2 user information from GitHub
     * 2. Checks if the user is a member of the frankframework organization
     * 3. Saves or updates the user in the database with their membership status
     * Note: Users are saved regardless of membership status. Authorization checks
     * are performed at the API endpoint level (see AuthenticationController).
     *
     * @param userRequest the OAuth2 user request containing access token and user details
     * @return OAuth2User the loaded and authenticated user
     * @throws OAuth2AuthenticationException if OAuth2 authentication fails or user data cannot be processed
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User;
        try {
            oauth2User = super.loadUser(userRequest);
        } catch (OAuth2AuthenticationException e) {
            log.error("Failed to load OAuth2 user from GitHub: {}", e.getMessage(), e);
            throw e;
        }

        String accessToken = userRequest.getAccessToken().getTokenValue();
        GitHubOAuthAttributes attributes = GitHubOAuthAttributes.from(oauth2User.getAttributes());

        log.info(
                "GitHub OAuth login attempt for user: '{}' (GitHub ID: {})",
                attributes.username(),
                attributes.githubId());

        boolean isFrankFrameworkMember = checkOrganizationMembership(accessToken, attributes.username());

        saveOrUpdateUser(attributes, isFrankFrameworkMember);

        log.info(
                "User '{}' successfully authenticated and saved user: {})",
                attributes.username(),
                isFrankFrameworkMember);

        return oauth2User;
    }

    /**
     * Checks if a user is a member of the frankframework organization using their OAuth token.
     * Handles failures gracefully by marking user as non-member rather than blocking authentication.
     *
     * @param accessToken the user's OAuth access token (required)
     * @param username the username (required)
     * @return true if the user is a verified member, false if not a member or if verification fails
     */
    private boolean checkOrganizationMembership(String accessToken, String username) {
        try {
            boolean isMember = gitHubRestClient.checkOAuthUserOrganizationMembership(accessToken, username);

            if (isMember) {
                log.info("User '{}' verified as a frankframework organization member", username);
            } else {
                log.warn("User '{}' is NOT a frankframework organization member", username);
            }

            return isMember;

        } catch (GitHubRestClientException e) {
            log.error(
                    "Failed to verify organization membership for user '{}' - marking as non-member. " + "Reason: {}",
                    username,
                    e.getMessage(),
                    e);
            return false;
        }
    }

    /**
     * Saves a new user or updates an existing user in the database.
     * This persists the user's membership status for use by the API endpoints.
     * Handles concurrent user creation by catching unique constraint violations and retrying.
     *
     * @param attributes the GitHub user attributes (required)
     * @param isFrankFrameworkMember whether the user is a frankframework organization member
     * @throws OAuth2AuthenticationException if database operation fails
     */
    private void saveOrUpdateUser(GitHubOAuthAttributes attributes, boolean isFrankFrameworkMember) {
        try {
            User existingUser =
                    userRepository.findByGithubId(attributes.githubId()).orElse(null);
            boolean isNewUser = existingUser == null;

            User user = userMapper.toEntity(attributes, existingUser, isFrankFrameworkMember);
            User savedUser = userRepository.save(user);

            log.info(
                    "User '{}' {} in database (ID: {}, frankframework member: {})",
                    attributes.username(),
                    isNewUser ? "created" : "updated",
                    savedUser.getId(),
                    isFrankFrameworkMember);

        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "Concurrent user creation detected for '{}' (GitHub ID: {}). Retrying with update.",
                    attributes.username(),
                    attributes.githubId());

            try {
                User existingUser = userRepository
                        .findByGithubId(attributes.githubId())
                        .orElseThrow(() -> new IllegalStateException(
                                "User should exist after constraint violation but was not found"));

                User user = userMapper.toEntity(attributes, existingUser, isFrankFrameworkMember);
                User savedUser = userRepository.save(user);

                log.info(
                        "User '{}' updated in database after concurrent creation (ID: {}, frankframework member: {})",
                        attributes.username(),
                        savedUser.getId(),
                        isFrankFrameworkMember);

            } catch (Exception retryException) {
                log.error(
                        "Failed to update user '{}' after concurrent creation: {}",
                        attributes.username(),
                        retryException.getMessage(),
                        retryException);
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("user_save_failed"),
                        String.format("Failed to save user '%s' to database after retry", attributes.username()),
                        retryException);
            }

        } catch (Exception e) {
            log.error("Failed to save user '{}' to database: {}", attributes.username(), e.getMessage(), e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_save_failed"),
                    String.format("Failed to save user '%s' to database", attributes.username()),
                    e);
        }
    }
}
