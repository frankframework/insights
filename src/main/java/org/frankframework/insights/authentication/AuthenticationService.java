package org.frankframework.insights.authentication;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.exception.ApiException;
import org.frankframework.insights.user.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Get user information from OAuth2 principal
     * Used by the /api/auth/user endpoint to return current user info
     *
     * @param principal OAuth2User from Spring Security (guaranteed non-null by Spring Security)
     * @return UserResponseDTO with user information
     * @throws ForbiddenException if authenticated but not a frankframework member
     */
    public UserResponseDTO getUserInfo(OAuth2User principal) throws ApiException {
        GitHubOAuthAttributes attributes = GitHubOAuthAttributes.from(principal.getAttributes());
        Optional<User> user = userRepository.findByGithubId(attributes.githubId());

        if (user.isEmpty() || !user.get().isFrankFrameworkMember()) {
            throw new ForbiddenException(
                    "Access denied. You must be a member of the frankframework organization on GitHub.");
        }

        return userMapper.toResponseDTO(attributes, user.get());
    }
}
