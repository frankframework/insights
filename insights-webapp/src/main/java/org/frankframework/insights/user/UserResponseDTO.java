package org.frankframework.insights.user;

/**
 * DTO for user information returned to the frontend via API endpoints
 */
public record UserResponseDTO(Long githubId, String username, String avatarUrl, boolean isFrankFrameworkMember) {}
