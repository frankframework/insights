package org.frankframework.insights.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.BranchDTO;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.graphql.GraphQLClient;
import org.frankframework.insights.issue.IssueDTO;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;
import org.frankframework.insights.pullrequest.PullRequestDTO;
import org.frankframework.insights.release.ReleaseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * GitHubClient is a GraphQL client for interacting with the GitHub API.
 */
@Component
@Slf4j
public class GitHubClient extends GraphQLClient {
    private final ObjectMapper objectMapper;

    public GitHubClient(GitHubProperties gitHubProperties, ObjectMapper objectMapper) {
        super(gitHubProperties.getUrl(), gitHubProperties.getSecret());
        this.objectMapper = objectMapper;
    }

	/**
	 * Fetches repository statistics from GitHub.
	 * @return GitHubRepositoryStatisticsDTO containing repository statistics
	 * @throws GitHubClientException if an error occurs during the request
	 */
    public GitHubRepositoryStatisticsDTO getRepositoryStatistics() throws GitHubClientException {
        GitHubRepositoryStatisticsDTO repositoryStatistics = fetchSingleEntity(
                GitHubQueryConstants.REPOSITORY_STATISTICS, new HashMap<>(), GitHubRepositoryStatisticsDTO.class);
        log.info("Fetched repository statistics from GitHub");
        return repositoryStatistics;
    }

	/**
	 * Fetches labels from GitHub.
	 * @return Set of LabelDTO containing labels
	 * @throws GitHubClientException if an error occurs during the request
	 */
    public Set<LabelDTO> getLabels() throws GitHubClientException {
        Set<LabelDTO> labels = getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
        log.info("Successfully fetched {} labels from GitHub", labels.size());
        return labels;
    }

	/**
	 * Fetches milestones from GitHub.
	 * @return Set of MilestoneDTO containing milestones
	 * @throws GitHubClientException if an error occurs during the request
	 */
    public Set<MilestoneDTO> getMilestones() throws GitHubClientException {
        Set<MilestoneDTO> milestones =
                getEntities(GitHubQueryConstants.MILESTONES, new HashMap<>(), MilestoneDTO.class);
        log.info("Successfully fetched {} milestones from GitHub", milestones.size());
        return milestones;
    }

	/**
	 * Fetches branches from GitHub.
	 * @return Set of BranchDTO containing branches
	 * @throws GitHubClientException if an error occurs during the request
	 */
    public Set<BranchDTO> getBranches() throws GitHubClientException {
        Set<BranchDTO> branches = getEntities(GitHubQueryConstants.BRANCHES, new HashMap<>(), BranchDTO.class);
        log.info("Successfully fetched {} branches from GitHub", branches.size());
        return branches;
    }

	/**
	 * Fetches issues from GitHub.
	 * @return Set of IssueDTO containing issues
	 * @throws GitHubClientException if an error occurs during the request
	 */
    public Set<IssueDTO> getIssues() throws GitHubClientException {
        Set<IssueDTO> issues = getEntities(GitHubQueryConstants.ISSUES, new HashMap<>(), IssueDTO.class);
        log.info("Successfully fetched {} issues from GitHub", issues.size());
        return issues;
    }

	/**
	 * Fetches pull requests for a specific branch from GitHub.
	 * @param branchName the name of the branch
	 * @return Set of PullRequestDTO containing pull requests for the specified branch
	 * @throws GitHubClientException if an error occurs during the request
	 */
    public Set<PullRequestDTO> getBranchPullRequests(String branchName) throws GitHubClientException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("branchName", branchName);
        log.info("Started fetching pull requests from GitHub for branch with name: {}", branchName);

        Set<PullRequestDTO> pullRequests =
                getEntities(GitHubQueryConstants.BRANCH_PULLS, variables, PullRequestDTO.class);
        log.info(
                "Successfully fetched {} pull requests from GitHub for branch with name: {}",
                pullRequests.size(),
                branchName);
        return pullRequests;
    }

	/**
	 * Fetches releases from GitHub.
	 * @return Set of ReleaseDTO containing releases
	 * @throws GitHubClientException if an error occurs during the request
	 */
    public Set<ReleaseDTO> getReleases() throws GitHubClientException {
        Set<ReleaseDTO> releases = getEntities(GitHubQueryConstants.RELEASES, new HashMap<>(), ReleaseDTO.class);
        log.info("Successfully fetched {} releases from GitHub", releases.size());
        return releases;
    }

	/**
	 * Fetches entities from GitHub using a paginated GraphQL query.
	 * @param query the GraphQL query to execute
	 * @param queryVariables the variables for the query
	 * @param entityType the type of entity to fetch
	 * @return a Set of entities of the specified type
	 * @param <T> the type of entity
	 * @throws GitHubClientException if an error occurs during the request
	 */
    private <T> Set<T> getEntities(GitHubQueryConstants query, Map<String, Object> queryVariables, Class<T> entityType)
            throws GitHubClientException {
        Set<T> allEntities = new HashSet<>();
        String cursor = null;
        boolean hasNextPage = true;

        while (hasNextPage) {
            queryVariables.put("after", cursor);

            GitHubPaginationDTO<T> response = fetchEntityPage(query, queryVariables, entityType);

            if (response == null || response.edges == null || response.edges.isEmpty()) {
                log.warn("Received empty response for query: {}", query);
                break;
            }

            Set<T> entities = response.edges.stream()
                    .map(edge -> objectMapper.convertValue(edge.node, entityType))
                    .collect(Collectors.toSet());

            allEntities.addAll(entities);
            log.info("Fetched {} additional entities with query: {}", entities.size(), query);

            hasNextPage = response.pageInfo != null && response.pageInfo.hasNextPage;
            cursor = (response.pageInfo != null) ? response.pageInfo.endCursor : null;
        }
        return allEntities;
    }

	/**
	 * Executes paginated GraphQL query on external GitHub server.
	 * @param query the GraphQL query to execute
	 * @param queryVariables the variables for the query
	 * @param entityType the type of entity to fetch
	 * @return a GitHubPaginationDTO containing the paginated response
	 * @param <T> the type of entity
	 * @throws GitHubClientException if an error occurs during the request
	 */
    protected <T> GitHubPaginationDTO<T> fetchEntityPage(
            GitHubQueryConstants query, Map<String, Object> queryVariables, Class<T> entityType)
            throws GitHubClientException {
        try {
            GitHubPaginationDTO<T> response = getGraphQlClient()
                    .documentName(query.getDocumentName())
                    .variables(queryVariables)
                    .retrieve(query.getRetrievePath())
                    .toEntity(new ParameterizedTypeReference<GitHubPaginationDTO<T>>() {})
                    .block();

            log.info("Successfully executed paginated GraphQL query: {}", query);
            return response;
        } catch (Exception e) {
            throw new GitHubClientException("Failed to execute GraphQL request for " + query, e);
        }
    }

	/**
	 * Executes a GraphQL query to fetch a single entity from GitHub.
	 * @param query the GraphQL query to execute
	 * @param queryVariables the variables for the query
	 * @param entityType the type of entity to fetch
	 * @return the entity of the specified type
	 * @param <T> the type of entity
	 * @throws GitHubClientException if an error occurs during the request
	 */
    protected <T> T fetchSingleEntity(GitHubQueryConstants query, Map<String, Object> queryVariables, Class<T> entityType)
            throws GitHubClientException {
        try {
            T response = getGraphQlClient()
                    .documentName(query.getDocumentName())
                    .variables(queryVariables)
                    .retrieve(query.getRetrievePath())
                    .toEntity(entityType)
                    .block();

            log.info("Successfully requested single object with GraphQL query: {}", query);
            return response;
        } catch (Exception e) {
            throw new GitHubClientException("Failed to execute GraphQL request for " + query, e);
        }
    }
}
