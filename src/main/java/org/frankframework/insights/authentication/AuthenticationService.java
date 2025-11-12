package org.frankframework.insights.authentication;

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
     * @param principal OAuth2User from Spring Security
     * @return UserResponseDTO with user information
     * @throws UnauthorizedException if not authenticated (not logged in)
     * @throws ForbiddenException if authenticated but not a frankframework member
     */
    public UserResponseDTO getUserInfo(OAuth2User principal) throws ApiException {
        if (principal == null) {
            throw new UnauthorizedException("You are not logged in. Please sign in with GitHub.");
        }

        GitHubOAuthAttributes attributes = GitHubOAuthAttributes.from(principal.getAttributes());
        User user = userRepository.findByGithubId(attributes.githubId()).orElse(null);

        UserResponseDTO userInfo = userMapper.toResponseDTO(attributes, user);

        if (!userInfo.isFrankFrameworkMember()) {
            throw new ForbiddenException(
                    "Access denied. You must be a member of the frankframework organization on GitHub.");
        }

        return userInfo;
    }
}
