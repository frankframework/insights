package org.frankframework.insights.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.BranchDTO;
import org.frankframework.insights.commit.CommitDTO;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.graphql.GraphQLClient;
import org.frankframework.insights.issue.IssueDTO;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;
import org.frankframework.insights.pullrequest.PullRequestDTO;
import org.frankframework.insights.release.ReleaseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GitHubClient extends GraphQLClient {
    private final ObjectMapper objectMapper;

    public GitHubClient(GitHubProperties gitHubProperties, ObjectMapper objectMapper) {
        super(gitHubProperties.getUrl(), gitHubProperties.getSecret());
        this.objectMapper = objectMapper;
    }

    public GitHubRepositoryStatisticsDTO getRepositoryStatistics() throws GitHubClientException {
        GitHubRepositoryStatisticsDTO repositoryStatistics = fetchSingleEntity(
                GitHubQueryConstants.REPOSITORY_STATISTICS, new HashMap<>(), GitHubRepositoryStatisticsDTO.class);
        log.info("Fetched repository statistics from GitHub");
        return repositoryStatistics;
    }

    public Set<LabelDTO> getLabels() throws GitHubClientException {
        Set<LabelDTO> labels = getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
        log.info("Successfully fetched {} labels from GitHub", labels.size());
        return labels;
    }

    public Set<MilestoneDTO> getMilestones() throws GitHubClientException {
        Set<MilestoneDTO> milestones =
                getEntities(GitHubQueryConstants.MILESTONES, new HashMap<>(), MilestoneDTO.class);
        log.info("Successfully fetched {} milestones from GitHub", milestones.size());
        return milestones;
    }

    public Set<BranchDTO> getBranches() throws GitHubClientException {
        Set<BranchDTO> branches = getEntities(GitHubQueryConstants.BRANCHES, new HashMap<>(), BranchDTO.class);
        log.info("Successfully fetched {} branches from GitHub", branches.size());
        return branches;
    }

    public Set<CommitDTO> getBranchCommits(String branchName) throws GitHubClientException {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("branchName", branchName);
        log.info("Started fetching commits from GitHub for branch with name: {}", branchName);

        Set<CommitDTO> commits = getEntities(GitHubQueryConstants.BRANCH_COMMITS, variables, CommitDTO.class);
        log.info("Successfully fetched {} commits from GitHub for branch with name: {}", commits.size(), branchName);
        return commits;
    }

    public Set<IssueDTO> getIssues() throws GitHubClientException {
        Set<IssueDTO> issues = getEntities(GitHubQueryConstants.ISSUES, new HashMap<>(), IssueDTO.class);
        log.info("Successfully fetched {} issues from GitHub", issues.size());
        return issues;
    }

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

    public Set<ReleaseDTO> getReleases() throws GitHubClientException {
        Set<ReleaseDTO> releases = getEntities(GitHubQueryConstants.RELEASES, new HashMap<>(), ReleaseDTO.class);
        log.info("Successfully fetched {} releases from GitHub", releases.size());
        return releases;
    }

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

    private <T> GitHubPaginationDTO<T> fetchEntityPage(
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

    private <T> T fetchSingleEntity(GitHubQueryConstants query, Map<String, Object> queryVariables, Class<T> entityType)
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
