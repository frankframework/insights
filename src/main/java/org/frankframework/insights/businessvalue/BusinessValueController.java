package org.frankframework.insights.businessvalue;

import jakarta.validation.Valid;
import java.util.List;
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
 * Business values are release-scoped: each release has its own set of business values.
 * All endpoints require FrankFramework member authentication via OAuth2,
 * except /release/{releaseId} (GET) which is public.
 */
@RestController
@RequestMapping("/business-value")
@Slf4j
public class BusinessValueController {

    private final BusinessValueService businessValueService;

    public BusinessValueController(BusinessValueService businessValueService) {
        this.businessValueService = businessValueService;
    }

    /**
     * Retrieves all business values for a specific release.
     * This is the primary endpoint for fetching business values — they are release-scoped.
     * Public endpoint, no authentication required.
     *
     * @param releaseId the release ID
     * @return list of business values for the release
     */
    @GetMapping("/release/{releaseId}")
    public ResponseEntity<List<BusinessValueResponse>> getBusinessValuesByReleaseId(@PathVariable String releaseId) {
        log.info("Fetching business values for release with id: {}", releaseId);
        List<BusinessValueResponse> responses = businessValueService.getBusinessValuesByReleaseId(releaseId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves a specific business value by ID with all connected issues.
     * Requires authentication.
     *
     * @param id the UUID of the business value
     * @param principal the authenticated user
     * @return the business value response with connected issues
     * @throws BusinessValueNotFoundException if the business value is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessValueResponse> getBusinessValueById(
            @PathVariable UUID id, @AuthenticationPrincipal OAuth2User principal)
            throws BusinessValueNotFoundException {
        log.info("User {} is fetching business value with id: {}", principal.getAttribute("login"), id);

        BusinessValueResponse response = businessValueService.getBusinessValueById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new business value for a specific release.
     * Title must be unique within the release.
     * Requires authentication.
     *
     * @param request the business value request (includes releaseId)
     * @param principal the authenticated user
     * @return the created business value response
     * @throws ApiException if creation fails
     */
    @PostMapping
    public ResponseEntity<BusinessValueResponse> createBusinessValue(
            @Valid @RequestBody BusinessValueRequest request, @AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        log.info(
                "User {} is creating a new business value '{}' for release {}",
                principal.getAttribute("login"),
                request.title(),
                request.releaseId());

        BusinessValueResponse response = businessValueService.createBusinessValue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates a business value's details (title and/or description).
     * Title must remain unique within the release.
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
            @Valid @RequestBody UpdateBusinessValueRequest request,
            @AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        log.info("User {} is updating business value with id: {}", principal.getAttribute("login"), id);

        BusinessValueResponse response = businessValueService.updateBusinessValue(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a business value and disconnects all its issues.
     * Requires authentication.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusinessValue(
            @PathVariable UUID id, @AuthenticationPrincipal OAuth2User principal)
            throws BusinessValueNotFoundException {
        log.info("User {} is deleting business value with id: {}", principal.getAttribute("login"), id);
        businessValueService.deleteBusinessValue(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the issue connections for a business value by replacing them.
     * Disconnects all currently connected issues and connects the new ones.
     * Sub-issues of connected parent issues are automatically included.
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

    /**
     * Duplicates all business values from a source release into the target release.
     * Only title and description are copied — no issue connections are carried over.
     * Business values whose title already exists in the target release are skipped.
     * Requires authentication.
     *
     * @param targetReleaseId the release to copy business values into
     * @param request contains the sourceReleaseId
     * @param principal the authenticated user
     * @return list of newly created business value responses
     */
    @PostMapping("/release/{targetReleaseId}/duplicate")
    public ResponseEntity<List<BusinessValueResponse>> duplicateBusinessValues(
            @PathVariable String targetReleaseId,
            @Valid @RequestBody DuplicateBusinessValuesRequest request,
            @AuthenticationPrincipal OAuth2User principal)
            throws ApiException {
        log.info(
                "User {} is duplicating business values from release {} to release {}",
                principal.getAttribute("login"),
                request.sourceReleaseId(),
                targetReleaseId);

        List<BusinessValueResponse> responses = businessValueService.duplicateBusinessValues(targetReleaseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}
