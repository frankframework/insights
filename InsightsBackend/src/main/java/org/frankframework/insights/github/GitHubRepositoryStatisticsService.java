package org.frankframework.insights.github;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitHubRepositoryStatisticsService {

    private final GitHubClient gitHubClient;

    @Getter
    private GitHubRepositoryStatisticsDTO gitHubRepositoryStatisticsDTO;

    public GitHubRepositoryStatisticsService(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

	/**
	 * Fetches the repository statistics from GitHub and stores them in the service.
	 * @throws GitHubClientException if an error occurs during the request
	 */
    public void fetchRepositoryStatistics() throws GitHubClientException {
        gitHubRepositoryStatisticsDTO = gitHubClient.getRepositoryStatistics();
    }
}
