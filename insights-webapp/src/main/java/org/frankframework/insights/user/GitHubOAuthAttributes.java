package org.frankframework.insights.user;

import java.util.Map;

/**
 * Internal DTO to extract and hold GitHub OAuth user attributes
 * Used to avoid code duplication when parsing OAuth2User attributes
 */
public record GitHubOAuthAttributes(Long githubId, String username, String avatarUrl) {

    /**
     * Extract GitHub user attributes from OAuth2User attributes map
     */
    public static GitHubOAuthAttributes from(Map<String, Object> attributes) {
        Long githubId = ((Number) attributes.get("id")).longValue();
        String username = (String) attributes.get("login");
        String avatarUrl = (String) attributes.get("avatar_url");

        return new GitHubOAuthAttributes(githubId, username, avatarUrl);
    }
}
