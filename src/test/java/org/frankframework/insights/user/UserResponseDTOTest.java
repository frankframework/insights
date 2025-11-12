package org.frankframework.insights.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserResponseDTOTest {

    @Test
    void constructor_createsRecordCorrectly() {
        // Given
        Long githubId = 12345L;
        String username = "testuser";
        String avatarUrl = "https://avatar.url";
        boolean isMember = true;

        // When
        UserResponseDTO dto = new UserResponseDTO(githubId, username, avatarUrl, isMember);

        // Then
        assertThat(dto.githubId()).isEqualTo(githubId);
        assertThat(dto.username()).isEqualTo(username);
        assertThat(dto.avatarUrl()).isEqualTo(avatarUrl);
        assertThat(dto.isFrankFrameworkMember()).isTrue();
    }

    @Test
    void equality_worksCorrectly() {
        // Given
        UserResponseDTO dto1 = new UserResponseDTO(12345L, "user", "url", true);
        UserResponseDTO dto2 = new UserResponseDTO(12345L, "user", "url", true);
        UserResponseDTO dto3 = new UserResponseDTO(99999L, "other", "other-url", false);

        // Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        // Given
        UserResponseDTO dto = new UserResponseDTO(12345L, "testuser", "https://avatar.url", true);

        // When
        String toString = dto.toString();

        // Then
        assertThat(toString).contains("12345");
        assertThat(toString).contains("testuser");
        assertThat(toString).contains("https://avatar.url");
        assertThat(toString).contains("true");
    }
}
