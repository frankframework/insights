package org.frankframework.insights.github;

import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void fetchRepositoryStatistics() throws GitHubClientException {
        gitHubRepositoryStatisticsDTO = gitHubClient.getRepositoryStatistics();
    }
}
