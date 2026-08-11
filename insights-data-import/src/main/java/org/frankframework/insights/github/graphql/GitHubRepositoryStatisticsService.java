package org.frankframework.insights.github.graphql;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitHubRepositoryStatisticsService {

    private final GitHubGraphQLClient gitHubGraphQLClient;

    @Getter
    private GitHubRepositoryStatisticsDTO gitHubRepositoryStatisticsDTO;

    public GitHubRepositoryStatisticsService(GitHubGraphQLClient gitHubGraphQLClient) {
        this.gitHubGraphQLClient = gitHubGraphQLClient;
    }

    /**
     * Fetches the repository statistics from GitHub and stores them in the service.
     * @throws GitHubGraphQLClientException if an error occurs during the request
     */
    public void fetchRepositoryStatistics() throws GitHubGraphQLClientException {
        gitHubRepositoryStatisticsDTO = gitHubGraphQLClient.getRepositoryStatistics();
    }
}
