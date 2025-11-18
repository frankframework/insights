package org.frankframework.insights.businessvalue;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing business values.
 * All endpoints require FrankFramework member authentication via OAuth2,
 * except /by-release/{releaseId} which is public.
 */
@RestController
@RequestMapping("/api/business-value")
@Slf4j
public class BusinessValueController {

    private final BusinessValueService businessValueService;

    public BusinessValueController(BusinessValueService businessValueService) {
        this.businessValueService = businessValueService;
    }

    /**
     * Creates a new business value.
     * Requires authentication.
     *
     * @param request the business value request
     * @param principal the authenticated user (ensures FrankFramework member access)
     * @return the created business value response
     * @throws ApiException if creation fails
     */
    @PostMapping
    public ResponseEntity<BusinessValueResponse> createBusinessValue(
            @Valid @RequestBody BusinessValueRequest request, @AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        log.info(
                "User {} is creating a new business value with name: {}",
                principal.getAttribute("login"),
                request.name());

        BusinessValueResponse response = businessValueService.createBusinessValue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all business values associated with issues in a specific release.
     * This is a public endpoint that does not require authentication.
     *
     * @param releaseId the ID of the release
     * @return set of business values associated with the release's issues
     * @throws ApiException if the release is not found
     */
    @GetMapping("/release/{releaseId}")
    public ResponseEntity<Set<BusinessValueResponse>> getBusinessValuesByReleaseId(@PathVariable String releaseId)
            throws ApiException {
        log.info("Fetching business values for release with id: {}", releaseId);

        Set<BusinessValueResponse> responses = businessValueService.getBusinessValuesByReleaseId(releaseId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves all business values.
     * Requires authentication.
     *
     * @param principal the authenticated user
     * @return set of all business values
     */
    @GetMapping
    public ResponseEntity<Set<BusinessValueResponse>> getAllBusinessValues(
            @AuthenticationPrincipal OAuth2User principal) {
        log.info("User {} is fetching all business values", Optional.ofNullable(principal.getAttribute("login")));

        Set<BusinessValueResponse> responses = businessValueService.getAllBusinessValues();
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves a specific business value by ID with all connected issues.
     * Requires authentication.
     *
     * @param id the UUID of the business value
     * @param principal the authenticated user
     * @return the business value response with connected issues
     * @throws ApiException if the business value is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessValueResponse> getBusinessValueById(
            @PathVariable UUID id, @AuthenticationPrincipal OAuth2User principal) throws ApiException {
        log.info("User {} is fetching business value with id: {}", principal.getAttribute("login"), id);

        BusinessValueResponse response = businessValueService.getBusinessValueById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates a business value's details (name and/or description).
     * Requires authentication.
     *
     * @param id the UUID of the business value
     * @param request the updated business value details
     * @param principal the authenticated user
     * @return the updated business value response
     * @throws ApiException if update fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<BusinessValueResponse> updateBusinessValue(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessValueRequest request,
            @AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        log.info("User {} is updating business value with id: {}", principal.getAttribute("login"), id);

        BusinessValueResponse response = businessValueService.updateBusinessValue(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Connects issues to a business value.
     * If an issue has sub-issues, all sub-issues are also connected automatically.
     * Requires authentication.
     *
     * @param id the UUID of the business value
     * @param request the request containing issue IDs to connect
     * @param principal the authenticated user
     * @return the updated business value response with connected issues
     * @throws ApiException if connection fails
     */
    @PostMapping("/{id}/connect-issues")
    public ResponseEntity<BusinessValueResponse> connectIssues(
            @PathVariable UUID id,
            @Valid @RequestBody ConnectIssuesRequest request,
            @AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        log.info(
                "User {} is connecting {} issues to business value with id: {}",
                principal.getAttribute("login"),
                request.issueIds().size(),
                id);

        BusinessValueResponse response = businessValueService.connectIssuesToBusinessValue(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Disconnects issues from a business value.
     * If an issue has sub-issues, all sub-issues are also disconnected automatically.
     * Requires authentication.
     *
     * @param id the UUID of the business value
     * @param request the request containing issue IDs to disconnect
     * @param principal the authenticated user
     * @return the updated business value response
     * @throws ApiException if disconnection fails
     */
    @PostMapping("/{id}/disconnect-issues")
    public ResponseEntity<BusinessValueResponse> disconnectIssues(
            @PathVariable UUID id,
            @Valid @RequestBody ConnectIssuesRequest request,
            @AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        log.info(
                "User {} is disconnecting {} issues from business value with id: {}",
                principal.getAttribute("login"),
                request.issueIds().size(),
                id);

        BusinessValueResponse response = businessValueService.disconnectIssuesFromBusinessValue(id, request.issueIds());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the issue connections for a business value by replacing them.
     * This endpoint disconnects all currently connected issues and connects the new ones.
     * If an issue has sub-issues, all sub-issues are also connected automatically.
     * Requires authentication.
     *
     * @param id the UUID of the business value
     * @param request the request containing new issue IDs to connect
     * @param principal the authenticated user
     * @return the updated business value response with new connections
     * @throws ApiException if update fails
     */
    @PutMapping("/{id}/issues")
    public ResponseEntity<BusinessValueResponse> updateIssueConnections(
            @PathVariable UUID id,
            @Valid @RequestBody ConnectIssuesRequest request,
            @AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        log.info(
                "User {} is replacing issue connections for business value with id: {}",
                principal.getAttribute("login"),
                id);

        BusinessValueResponse response = businessValueService.replaceIssueConnections(id, request);
        return ResponseEntity.ok(response);
    }
}
