package org.frankframework.insights.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserResponseDTOTest {

    @Test
    void constructor_createsRecordCorrectly() {
        Long githubId = 12345L;
        String username = "testuser";
        String avatarUrl = "https://avatar.url";
        boolean isMember = true;

        UserResponseDTO dto = new UserResponseDTO(githubId, username, avatarUrl, isMember);

        assertThat(dto.githubId()).isEqualTo(githubId);
        assertThat(dto.username()).isEqualTo(username);
        assertThat(dto.avatarUrl()).isEqualTo(avatarUrl);
        assertThat(dto.isFrankFrameworkMember()).isTrue();
    }

    @Test
    void equality_worksCorrectly() {
        UserResponseDTO dto1 = new UserResponseDTO(12345L, "user", "url", true);
        UserResponseDTO dto2 = new UserResponseDTO(12345L, "user", "url", true);
        UserResponseDTO dto3 = new UserResponseDTO(99999L, "other", "other-url", false);

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        UserResponseDTO dto = new UserResponseDTO(12345L, "testuser", "https://avatar.url", true);

        String toString = dto.toString();

        assertThat(toString).contains("12345");
        assertThat(toString).contains("testuser");
        assertThat(toString).contains("https://avatar.url");
        assertThat(toString).contains("true");
    }
}
