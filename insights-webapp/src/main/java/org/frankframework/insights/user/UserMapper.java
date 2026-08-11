package org.frankframework.insights.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for User entity and related DTOs
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    /**
     * Map GitHubOAuthAttributes and User entity to UserResponseDTO
     */
    public UserResponseDTO toResponseDTO(GitHubOAuthAttributes attributes, User user) {
        boolean isMember = user != null && user.isFrankFrameworkMember();
        return new UserResponseDTO(attributes.githubId(), attributes.username(), attributes.avatarUrl(), isMember);
    }

    /**
     * Map GitHubOAuthAttributes to User entity
     * Updates an existing user or creates a new one with the OAuth attributes
     *
     * @param attributes GitHub OAuth user attributes
     * @param user Existing user entity or null to create new
     * @param isFrankFrameworkMember Whether the user is a member of the frankframework organization
     * @return User entity
     */
    public User toEntity(GitHubOAuthAttributes attributes, User user, boolean isFrankFrameworkMember) {
        if (user == null) {
            return User.builder()
                    .githubId(attributes.githubId())
                    .username(attributes.username())
                    .avatarUrl(attributes.avatarUrl())
                    .isFrankFrameworkMember(isFrankFrameworkMember)
                    .build();
        }

        user.setGithubId(attributes.githubId());
        user.setUsername(attributes.username());
        user.setAvatarUrl(attributes.avatarUrl());
        user.setFrankFrameworkMember(isFrankFrameworkMember);

        return user;
    }
}
