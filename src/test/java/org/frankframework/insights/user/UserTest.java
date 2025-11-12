package org.frankframework.insights.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void builder_createsUserCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Long githubId = 12345L;
        String username = "testuser";
        String avatarUrl = "https://avatar.url";
        boolean isMember = true;
        OffsetDateTime now = OffsetDateTime.now();

        // When
        User user = User.builder()
                .id(id)
                .githubId(githubId)
                .username(username)
                .avatarUrl(avatarUrl)
                .isFrankFrameworkMember(isMember)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getGithubId()).isEqualTo(githubId);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(user.isFrankFrameworkMember()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void onCreate_setsTimestamps() {
        // Given
        User user = User.builder()
                .githubId(12345L)
                .username("testuser")
                .isFrankFrameworkMember(true)
                .build();

        OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

        // When
        user.onCreate();

        // Then
        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isBetween(before, after);
        assertThat(user.getUpdatedAt()).isBetween(before, after);
    }

    @Test
    void onUpdate_updatesTimestamp() {
        // Given
        OffsetDateTime oldTime = OffsetDateTime.now().minusHours(1);
        User user = User.builder()
                .githubId(12345L)
                .username("testuser")
                .isFrankFrameworkMember(true)
                .createdAt(oldTime)
                .updatedAt(oldTime)
                .build();

        OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

        // When
        user.onUpdate();

        // Then
        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);
        assertThat(user.getCreatedAt()).isEqualTo(oldTime); // Should not change
        assertThat(user.getUpdatedAt()).isBetween(before, after); // Should be updated
    }

    @Test
    void setters_modifyFields() {
        // Given
        User user = User.builder()
                .githubId(12345L)
                .username("oldname")
                .isFrankFrameworkMember(false)
                .build();

        // When
        user.setUsername("newname");
        user.setAvatarUrl("https://new-avatar.url");
        user.setFrankFrameworkMember(true);

        // Then
        assertThat(user.getUsername()).isEqualTo("newname");
        assertThat(user.getAvatarUrl()).isEqualTo("https://new-avatar.url");
        assertThat(user.isFrankFrameworkMember()).isTrue();
    }
}
