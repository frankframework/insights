package org.frankframework.insights.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.exception.ApiException;
import org.frankframework.insights.user.UserResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Get the current authenticated user information.
     * Returns:
     * - 200 with user data if authenticated and authorized
     * - 401 with error message if not authenticated (not logged in)
     * - 403 with error message if authenticated but not authorized (not a frankframework member)
     */
    @GetMapping("/user")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        UserResponseDTO userInfo = authenticationService.getUserInfo(principal);
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Logout the current user
     * Invalidates the session and clears authentication
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return ResponseEntity.ok().build();
    }
}
