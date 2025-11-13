package org.frankframework.insights.github.rest;

import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.client.rest.RestClient;
import org.frankframework.insights.common.properties.GitHubProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GitHubRestClient extends RestClient {

    public GitHubRestClient(GitHubProperties gitHubProperties) {
        super(
                gitHubProperties.getRest().getUrl(),
                builder -> builder.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json"));
    }

    /**
     * Check if an authenticated OAuth user is a member of the frankframework organization.
     * This method uses the user's OAuth access token to check their own membership,
     * which works for both public and private memberships.
     * Requires the OAuth token to have 'read:org' scope.
     *
     * @param accessToken the OAuth access token of the user
     * @param username the GitHub username of the user
     * @return true if the user is a member of the frankframework organization, false otherwise
     * @throws GitHubRestClientException if the request fails or the token is invalid
     */
    public boolean checkOAuthUserOrganizationMembership(String accessToken, String username)
            throws GitHubRestClientException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new GitHubRestClientException(
                    "Access token cannot be null or empty", new IllegalArgumentException("Invalid access token"));
        }

        if (username == null || username.isBlank()) {
            throw new GitHubRestClientException(
                    "Username cannot be null or empty", new IllegalArgumentException("Invalid Username"));
        }

        try {
            log.debug("Checking OAuth organization membership for user '{}' with provided OAuth token", username);

            Boolean isMember = getRestClient()
                    .get()
                    .uri("/user/memberships/orgs/frankframework")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .exchangeToMono(response -> handleMembershipResponse(response, username))
                    .block();

            log.info(
                    "OAuth membership check for user '{}': {} (endpoint: /user/memberships/orgs/frankframework)",
                    username,
                    Boolean.TRUE.equals(isMember) ? "MEMBER" : "NOT A MEMBER");
            return Boolean.TRUE.equals(isMember);

        } catch (Exception e) {
            log.error("Failed to check OAuth organization membership for user '{}': {}", username, e.getMessage(), e);
            throw new GitHubRestClientException(
                    String.format(
                            "Failed to check OAuth organization membership for user '%s'. "
                                    + "Ensure the OAuth token has 'read:org' scope",
                            username),
                    e);
        }
    }

    /**
     * Helper method to handle GitHub API responses for membership checks.
     * Distinguishes between "not a member" (404) and actual errors.
     *
     * @param response the ClientResponse from GitHub API
     * @param username the username being checked (for logging)
     * @return Mono<Boolean> true if member (2xx status), false if not a member (404), error for other cases
     */
    private Mono<Boolean> handleMembershipResponse(ClientResponse response, String username) {
        HttpStatusCode status = response.statusCode();

        if (status.is2xxSuccessful()) {
            log.info("{} membership check for user {} returned success ({})", "OAuth", username, status.value());
            return Mono.just(true);
        } else if (status.value() == HttpStatus.NOT_FOUND.value()) {
            log.warn(
                    "{} membership check for user {} returned 404 - not a member or private membership",
                    "OAuth",
                    username);
            return Mono.just(false);
        } else {
            return response.createException().flatMap(ex -> {
                log.error(
                        "{} membership check for user {} failed with status {}: {}",
                        "OAuth",
                        username,
                        status.value(),
                        ex.getMessage());
                return Mono.error(new GitHubRestClientException(
                        String.format(
                                "%s membership check failed with status %d for user '%s'",
                                "OAuth", status.value(), username),
                        ex));
            });
        }
    }
}
