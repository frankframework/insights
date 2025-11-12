package org.frankframework.insights.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GitHubOAuthAttributesTest {

    @Test
    public void from_withValidAttributes_extractsCorrectly() {
        Map<String, Object> attributes = Map.of(
                "id", 12345L,
                "login", "testuser",
                "avatar_url", "https://github.com/avatars/testuser.png");

        GitHubOAuthAttributes result = GitHubOAuthAttributes.from(attributes);

        assertThat(result).isNotNull();
        assertThat(result.githubId()).isEqualTo(12345L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.avatarUrl()).isEqualTo("https://github.com/avatars/testuser.png");
    }

    @Test
    public void from_withIntegerId_convertsToLong() {
        Map<String, Object> attributes = Map.of(
                "id", 12345,
                "login", "testuser",
                "avatar_url", "https://github.com/avatars/testuser.png");

        GitHubOAuthAttributes result = GitHubOAuthAttributes.from(attributes);

        assertThat(result.githubId()).isEqualTo(12345L);
    }

    @Test
    public void from_withMissingId_throwsException() {
        Map<String, Object> attributes = Map.of(
                "login", "testuser",
                "avatar_url", "https://github.com/avatars/testuser.png");

        assertThatThrownBy(() -> GitHubOAuthAttributes.from(attributes)).isInstanceOf(Exception.class);
    }

    @Test
    public void from_withMissingLogin_isEqualToNull() {
        Map<String, Object> attributes = Map.of("id", 12345L, "avatar_url", "https://github.com/avatars/testuser.png");

        assertThat(GitHubOAuthAttributes.from(attributes).username()).isEqualTo(null);
    }

    @Test
    public void from_withMissingAvatarUrl_isEqualToNull() {
        Map<String, Object> attributes = Map.of("id", 12345L, "login", "testuser");

        assertThat(GitHubOAuthAttributes.from(attributes).avatarUrl()).isEqualTo(null);
    }

    @Test
    public void from_withNullId_throwsException() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", null);
        attributes.put("login", "testuser");
        attributes.put("avatar_url", "https://github.com/avatars/testuser.png");

        assertThatThrownBy(() -> GitHubOAuthAttributes.from(attributes)).isInstanceOf(Exception.class);
    }

    @Test
    public void from_withNullLogin_isEqualToNullOnDefault() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("login", null);
        attributes.put("avatar_url", "https://github.com/avatars/testuser.png");

        assertThat(GitHubOAuthAttributes.from(attributes).username()).isEqualTo(null);
    }

    @Test
    public void record_equalityWorks() {
        GitHubOAuthAttributes attrs1 = new GitHubOAuthAttributes(12345L, "testuser", "https://avatar.url");
        GitHubOAuthAttributes attrs2 = new GitHubOAuthAttributes(12345L, "testuser", "https://avatar.url");
        GitHubOAuthAttributes attrs3 = new GitHubOAuthAttributes(99999L, "otheruser", "https://other.url");

        assertThat(attrs1).isEqualTo(attrs2);
        assertThat(attrs1).isNotEqualTo(attrs3);
        assertThat(attrs1.hashCode()).isEqualTo(attrs2.hashCode());
    }
}
