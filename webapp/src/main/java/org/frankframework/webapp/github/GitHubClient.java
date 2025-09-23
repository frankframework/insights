package org.frankframework.webapp.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.webapp.branch.BranchDTO;
import org.frankframework.webapp.common.configuration.properties.GitHubProperties;
import org.frankframework.webapp.graphql.GraphQLClient;
import org.frankframework.webapp.issue.IssueDTO;
import org.frankframework.webapp.issuetype.IssueTypeDTO;
import org.frankframework.webapp.label.LabelDTO;
import org.frankframework.webapp.milestone.MilestoneDTO;
import org.frankframework.webapp.pullrequest.PullRequestDTO;
import org.frankframework.webapp.release.ReleaseDTO;
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
     * Fetches issue types from GitHub
     * @return Set of IssueTypeDTO containing issue types
     * @throws GitHubClientException of an error occurs during the request
     */
    public Set<IssueTypeDTO> getIssueTypes() throws GitHubClientException {
        Set<IssueTypeDTO> issueTypes =
                getEntities(GitHubQueryConstants.ISSUE_TYPES, new HashMap<>(), IssueTypeDTO.class);
        log.info("Successfully fetched {} issue types from GitHub", issueTypes.size());
        return issueTypes;
    }

    /**
     * Fetches issue priorities from GitHub.
     * @param projectId the ID of the project for which to fetch issue priorities
     * @return Set of GitHubSingleSelectDTO containing issue priorities
     * @throws GitHubClientException if an error occurs during the request
     */
    public Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> getIssuePriorities(String projectId)
            throws GitHubClientException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("projectId", projectId);
        log.info("Started fetching issue priorities from GitHub for project with id: [{}]", projectId);

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> issuePriorities =
                getNodes(GitHubQueryConstants.ISSUE_PRIORITIES, variables, new ParameterizedTypeReference<>() {});

        log.info(
                "Successfully fetched {} issue priorities from GitHub for project with id: [{}]",
                issuePriorities.size(),
                projectId);

        return issuePriorities;
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
     * Fetches entities from GitHub using a GraphQL query.
     * @param query the GraphQL query to execute
     * @param queryVariables the variables for the query
     * @param entityType the type of entity to fetch
     * @return Set of entities of the specified type
     * @param <T> the type of entity
     * @throws GitHubClientException if an error occurs during the request
     */
    protected <T> Set<T> getEntities(
            GitHubQueryConstants query, Map<String, Object> queryVariables, Class<T> entityType)
            throws GitHubClientException {
        return getPaginatedEntities(
                query,
                queryVariables,
                new ParameterizedTypeReference<GitHubPaginationDTO<T>>() {},
                dto -> dto.edges() == null
                        ? Set.of()
                        : dto.edges().stream()
                                .map(edge -> objectMapper.convertValue(edge.node(), entityType))
                                .collect(Collectors.toSet()),
                GitHubPaginationDTO::pageInfo);
    }

    /**
     * Fetches nodes from GitHub using a GraphQL query.
     * @param query the GraphQL query to execute
     * @param queryVariables the variables for the query
     * @param responseType the type of the response to expect
     * @return Set of GitHubSingleSelectDTO.SingleSelectObject containing nodes
     * @param <RAW> the raw	 response type
     * @throws GitHubClientException if an error occurs during the request
     */
    protected <RAW extends GitHubPrioritySingleSelectDTO>
            Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> getNodes(
                    GitHubQueryConstants query,
                    Map<String, Object> queryVariables,
                    ParameterizedTypeReference<RAW> responseType)
                    throws GitHubClientException {
        return getPaginatedEntities(
                query,
                queryVariables,
                responseType,
                dto -> dto.nodes() == null ? Set.of() : new HashSet<>(dto.nodes()),
                GitHubPrioritySingleSelectDTO::pageInfo);
    }

    /**
     * Executes a GraphQL query to fetch paginated entities from GitHub.
     * @param query the GraphQL query to execute
     * @param queryVariables the variables for the query
     * @param responseType the type of the response to expect
     * @param entityExtractor a function to extract entities from the response
     * @param pageInfoExtractor a function to extract pagination information from the response
     * @return Set of entities of the specified type
     * @param <RAW> the raw response type
     * @param <T> the type of entity to fetch
     * @throws GitHubClientException if an error occurs during the request
     */
    protected <RAW, T> Set<T> getPaginatedEntities(
            GitHubQueryConstants query,
            Map<String, Object> queryVariables,
            ParameterizedTypeReference<RAW> responseType,
            Function<RAW, Collection<T>> entityExtractor,
            Function<RAW, GitHubPageInfo> pageInfoExtractor)
            throws GitHubClientException {

        try {
            Set<T> allEntities = new HashSet<>();
            String cursor = null;
            boolean hasNextPage = true;

            while (hasNextPage) {
                queryVariables.put("after", cursor);

                RAW response = getGraphQlClient()
                        .documentName(query.getDocumentName())
                        .variables(queryVariables)
                        .retrieve(query.getRetrievePath())
                        .toEntity(responseType)
                        .block();

                if (response == null) {
                    log.warn("Received null response for query: {}", query);
                    break;
                }

                Collection<T> entities = entityExtractor.apply(response);
                if (entities == null || entities.isEmpty()) {
                    log.warn("Received empty entities for query: {}", query);
                    break;
                }

                allEntities.addAll(entities);
                log.info("Fetched {} entities with query: {}", entities.size(), query);

                GitHubPageInfo pageInfo = pageInfoExtractor.apply(response);
                hasNextPage = pageInfo != null && pageInfo.hasNextPage();
                cursor = (pageInfo != null) ? pageInfo.endCursor() : null;
            }
            return allEntities;
        } catch (Exception e) {
            throw new GitHubClientException("Failed to execute GraphQL request for " + query, e);
        }
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
     * Executes a GraphQL query to fetch a single entity from GitHub.
     * @param query the GraphQL query to execute
     * @param queryVariables the variables for the query
     * @param entityType the type of entity to fetch
     * @return the entity of the specified type
     * @param <T> the type of entity
     * @throws GitHubClientException if an error occurs during the request
     */
    protected <T> T fetchSingleEntity(
            GitHubQueryConstants query, Map<String, Object> queryVariables, Class<T> entityType)
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
