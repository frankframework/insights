package org.frankframework.insights.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.BranchDTO;
import org.frankframework.insights.common.client.graphql.GraphQLClient;
import org.frankframework.insights.common.client.graphql.GraphQLClientException;
import org.frankframework.insights.common.client.graphql.GraphQLConnectionDTO;
import org.frankframework.insights.common.client.graphql.GraphQLNodeDTO;
import org.frankframework.insights.common.client.graphql.GraphQLQuery;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.issue.IssueDTO;
import org.frankframework.insights.issuetype.IssueTypeDTO;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;
import org.frankframework.insights.pullrequest.PullRequestDTO;
import org.frankframework.insights.release.ReleaseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GitHubClient extends GraphQLClient {

    public GitHubClient(GitHubProperties gitHubProperties, ObjectMapper objectMapper) {
        super(
                gitHubProperties.getUrl(),
                builder -> {
                    if (gitHubProperties.getSecret() != null
                            && !gitHubProperties.getSecret().isEmpty()) {
                        builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + gitHubProperties.getSecret());
                    }
                },
                objectMapper);
    }

    /**
     * Fetches repository statistics from GitHub.
     * @return GitHubRepositoryStatisticsDTO containing repository statistics
     * @throws GitHubClientException if an error occurs during the request
     */
    public GitHubRepositoryStatisticsDTO getRepositoryStatistics() throws GitHubClientException {
        try {
            GitHubRepositoryStatisticsDTO repositoryStatisticsDTO = fetchSingleEntity(
                    GitHubQueryConstants.REPOSITORY_STATISTICS, new HashMap<>(), GitHubRepositoryStatisticsDTO.class);
            log.info("Fetched repository statistics from GitHub");
            return repositoryStatisticsDTO;
        } catch (GraphQLClientException e) {
            throw new GitHubClientException("Failed to fetch repository statistics from GitHub.", e);
        }
    }

    /**
     * Fetches labels from GitHub.
     * @return Set of LabelDTO containing labels
     * @throws GitHubClientException if an error occurs during the request
     */
    public Set<LabelDTO> getLabels() throws GitHubClientException {
        try {
            Set<LabelDTO> labels = fetchPaginatedViaRelay(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
            log.info("Successfully fetched {} labels from GitHub", labels.size());
            return labels;
        } catch (GraphQLClientException e) {
            throw new GitHubClientException("Failed to fetch labels from GitHub.", e);
        }
    }

    /**
     * Fetches milestones from GitHub.
     * @return Set of MilestoneDTO containing milestones
     * @throws GitHubClientException if an error occurs during the request
     */
    public Set<MilestoneDTO> getMilestones() throws GitHubClientException {
        try {
            Set<MilestoneDTO> milestones =
                    fetchPaginatedViaRelay(GitHubQueryConstants.MILESTONES, new HashMap<>(), MilestoneDTO.class);
            log.info("Successfully fetched {} milestones from GitHub", milestones.size());
            return milestones;
        } catch (GraphQLClientException e) {
            throw new GitHubClientException("Failed to fetch milestones from GitHub.", e);
        }
    }

    /**
     * Fetches issue types from GitHub
     * @return Set of IssueTypeDTO containing issue types
     * @throws GitHubClientException of an error occurs during the request
     */
    public Set<IssueTypeDTO> getIssueTypes() throws GitHubClientException {
        try {
            Set<IssueTypeDTO> issueTypes =
                    fetchPaginatedViaRelay(GitHubQueryConstants.ISSUE_TYPES, new HashMap<>(), IssueTypeDTO.class);
            log.info("Successfully fetched {} issueTypes from GitHub", issueTypes.size());
            return issueTypes;
        } catch (GraphQLClientException e) {
            throw new GitHubClientException("Failed to fetch issue types from GitHub.", e);
        }
    }

    /**
     * Fetches issue priorities from GitHub.
     * @param projectId the ID of the project for which to fetch issue priorities
     * @return Set of GitHubSingleSelectDTO containing issue priorities
     * @throws GitHubClientException if an error occurs during the request
     */
    public Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> getIssuePriorities(String projectId)
            throws GitHubClientException {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", projectId);
            return fetchPaginatedViaNodes(
                    GitHubQueryConstants.ISSUE_PRIORITIES,
                    variables,
                    GitHubPrioritySingleSelectDTO.SingleSelectObject.class);
        } catch (GraphQLClientException e) {
            throw new GitHubClientException("Failed to fetch issue priorities from GitHub.", e);
        }
    }

    /**
     * Fetches branches from GitHub.
     * @return Set of BranchDTO containing branches
     * @throws GitHubClientException if an error occurs during the request
     */
    public Set<BranchDTO> getBranches() throws GitHubClientException {
        try {
            Set<BranchDTO> branches =
                    fetchPaginatedViaRelay(GitHubQueryConstants.BRANCHES, new HashMap<>(), BranchDTO.class);
            log.info("Successfully fetched {} branches from GitHub", branches.size());
            return branches;
        } catch (GraphQLClientException e) {
            throw new GitHubClientException("Failed to fetch branches from GitHub.", e);
        }
    }

    /**
     * Fetches issues from GitHub.
     * @return Set of IssueDTO containing issues
     * @throws GitHubClientException if an error occurs during the request
     */
    public Set<IssueDTO> getIssues() throws GitHubClientException {
        try {
            Set<IssueDTO> issues = fetchPaginatedViaRelay(GitHubQueryConstants.ISSUES, new HashMap<>(), IssueDTO.class);
            log.info("Successfully fetched {} issues from GitHub", issues.size());
            return issues;
        } catch (GraphQLClientException e) {
            throw new GitHubClientException("Failed to fetch issues from GitHub.", e);
        }
    }

    /**
     * Fetches pull requests for a specific branch from GitHub.
     * @param branchName the name of the branch
     * @return Set of PullRequestDTO containing pull requests for the specified branch
     * @throws GitHubClientException if an error occurs during the request
     */
    public Set<PullRequestDTO> getBranchPullRequests(String branchName) throws GitHubClientException {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("branchName", branchName);
            Set<PullRequestDTO> pullRequests =
                    fetchPaginatedViaRelay(GitHubQueryConstants.BRANCH_PULLS, variables, PullRequestDTO.class);
            log.info(
                    "Successfully fetched {} pull requests for branch {} from GitHub", pullRequests.size(), branchName);
            return pullRequests;
        } catch (GraphQLClientException e) {
            throw new GitHubClientException(
                    String.format("Failed to fetch pull requests for branch '%s' from GitHub.", branchName), e);
        }
    }

    public Set<ReleaseDTO> getReleases() throws GitHubClientException {
        try {
            Set<ReleaseDTO> releases =
                    fetchPaginatedViaRelay(GitHubQueryConstants.RELEASES, new HashMap<>(), ReleaseDTO.class);
            log.info("Successfully fetched {} releases from GitHub", releases.size());
            return releases;
        } catch (GraphQLClientException e) {
            throw new GitHubClientException("Failed to fetch releases from GitHub.", e);
        }
    }

    /**
     * Helper to fetch paginated data from a standard Relay-style GraphQL connection.
     * @param query the GraphQL query to execute
     * @param queryVariables the variables for the GraphQL query
     * @param entityType the class type of the entities to fetch
     * @return Set of entities of type T
     * @param <T> the type of entities to fetch
     * @throws GraphQLClientException if an error occurs during the request
     */
    private <T> Set<T> fetchPaginatedViaRelay(
            GraphQLQuery query, Map<String, Object> queryVariables, Class<T> entityType) throws GraphQLClientException {
        ParameterizedTypeReference<GraphQLConnectionDTO<Map<String, Object>>> responseType =
                new ParameterizedTypeReference<>() {};

        Function<GraphQLConnectionDTO<Map<String, Object>>, Collection<Map<String, Object>>> collectionExtractor =
                connection -> connection.edges() == null
                        ? Set.of()
                        : connection.edges().stream().map(GraphQLNodeDTO::node).collect(Collectors.toList());

        return fetchPaginatedCollection(
                query, queryVariables, entityType, responseType, collectionExtractor, GraphQLConnectionDTO::pageInfo);
    }

    /**
     * Helper to fetch paginated data from a GraphQL connection using the 'nodes' field.
     * @param query the GraphQL query to execute
     * @param queryVariables the variables for the GraphQL query
     * @param entityType the class type of the entities to fetch
     * @return Set of entities of type T
     * @param <T> the type of entities to fetch
     * @throws GraphQLClientException if an error occurs during the request
     */
    private <T> Set<T> fetchPaginatedViaNodes(
            GraphQLQuery query, Map<String, Object> queryVariables, Class<T> entityType) throws GraphQLClientException {
        ParameterizedTypeReference<GitHubNodesDTO<Map<String, Object>>> responseType =
                new ParameterizedTypeReference<>() {};
        return fetchPaginatedCollection(
                query, queryVariables, entityType, responseType, GitHubNodesDTO::nodes, GitHubNodesDTO::pageInfo);
    }
}
