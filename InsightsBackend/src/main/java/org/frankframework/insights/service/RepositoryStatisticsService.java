package org.frankframework.insights.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.RepositoryStatisticsDTO;
import org.frankframework.insights.exceptions.clients.GitHubClientException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RepositoryStatisticsService {

    private final GitHubClient gitHubClient;

    @Getter
    private RepositoryStatisticsDTO repositoryStatisticsDTO;

    public RepositoryStatisticsService(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    @PostConstruct
    public void fetchRepositoryStatistics() throws GitHubClientException {
        repositoryStatisticsDTO = gitHubClient.getRepositoryStatistics();
    }
}
