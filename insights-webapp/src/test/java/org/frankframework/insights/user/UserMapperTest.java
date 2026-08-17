package org.frankframework.insights.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserMapperTest {

    private UserMapper userMapper;
    private GitHubOAuthAttributes attributes;

    @BeforeEach
    public void setUp() {
        userMapper = new UserMapper();
        attributes = new GitHubOAuthAttributes(12345L, "testuser", "https://github.com/avatars/testuser.png");
    }

    @Test
    public void toResponseDTO_whenUserIsMember_returnsMemberTrue() {
        User memberUser = User.builder()
                .id(UUID.randomUUID())
                .githubId(12345L)
                .username("testuser")
                .avatarUrl("https://github.com/avatars/testuser.png")
                .isFrankFrameworkMember(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        UserResponseDTO result = userMapper.toResponseDTO(attributes, memberUser);

        assertThat(result).isNotNull();
        assertThat(result.githubId()).isEqualTo(12345L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.avatarUrl()).isEqualTo("https://github.com/avatars/testuser.png");
        assertThat(result.isFrankFrameworkMember()).isTrue();
    }

    @Test
    public void toResponseDTO_whenUserIsNotMember_returnsMemberFalse() {
        User nonMemberUser = User.builder()
                .id(UUID.randomUUID())
                .githubId(12345L)
                .username("testuser")
                .avatarUrl("https://github.com/avatars/testuser.png")
                .isFrankFrameworkMember(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        UserResponseDTO result = userMapper.toResponseDTO(attributes, nonMemberUser);

        assertThat(result).isNotNull();
        assertThat(result.githubId()).isEqualTo(12345L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.avatarUrl()).isEqualTo("https://github.com/avatars/testuser.png");
        assertThat(result.isFrankFrameworkMember()).isFalse();
    }

    @Test
    public void toResponseDTO_whenUserIsNull_returnsMemberFalse() {
        UserResponseDTO result = userMapper.toResponseDTO(attributes, null);

        assertThat(result).isNotNull();
        assertThat(result.githubId()).isEqualTo(12345L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.avatarUrl()).isEqualTo("https://github.com/avatars/testuser.png");
        assertThat(result.isFrankFrameworkMember()).isFalse();
    }

    @Test
    public void toEntity_whenUserIsNull_createsNewUser() {
        User result = userMapper.toEntity(attributes, null, true);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getGithubId()).isEqualTo(12345L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getAvatarUrl()).isEqualTo("https://github.com/avatars/testuser.png");
        assertThat(result.isFrankFrameworkMember()).isTrue();
    }

    @Test
    public void toEntity_whenUserIsNull_createsNonMemberUser() {
        User result = userMapper.toEntity(attributes, null, false);

        assertThat(result).isNotNull();
        assertThat(result.getGithubId()).isEqualTo(12345L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getAvatarUrl()).isEqualTo("https://github.com/avatars/testuser.png");
        assertThat(result.isFrankFrameworkMember()).isFalse();
    }

    @Test
    public void toEntity_whenUserExists_updatesExistingUser() {
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .githubId(99999L)
                .username("oldusername")
                .avatarUrl("https://github.com/avatars/old.png")
                .isFrankFrameworkMember(false)
                .createdAt(OffsetDateTime.now().minusDays(30))
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .build();

        User result = userMapper.toEntity(attributes, existingUser, true);

        assertThat(result).isSameAs(existingUser);
        assertThat(result.getId()).isEqualTo(existingUser.getId());
        assertThat(result.getGithubId()).isEqualTo(12345L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getAvatarUrl()).isEqualTo("https://github.com/avatars/testuser.png");
        assertThat(result.isFrankFrameworkMember()).isTrue();
    }

    @Test
    public void toEntity_whenUserExists_canUpdateMembershipStatus() {
        User existingMember = User.builder()
                .id(UUID.randomUUID())
                .githubId(12345L)
                .username("testuser")
                .avatarUrl("https://github.com/avatars/testuser.png")
                .isFrankFrameworkMember(true)
                .createdAt(OffsetDateTime.now().minusDays(30))
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .build();

        User result = userMapper.toEntity(attributes, existingMember, false);

        assertThat(result.isFrankFrameworkMember()).isFalse();
    }
}
