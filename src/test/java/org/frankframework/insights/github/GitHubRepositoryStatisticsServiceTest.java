package org.frankframework.insights.github;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
import org.frankframework.insights.github.graphql.GitHubRefsDTO;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsService;
import org.frankframework.insights.github.graphql.GitHubTotalCountDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GitHubRepositoryStatisticsServiceTest {

    private GitHubGraphQLClient gitHubGraphQLClient;
    private GitHubRepositoryStatisticsService service;

    @BeforeEach
    public void setup() {
        gitHubGraphQLClient = mock(GitHubGraphQLClient.class);
        service = new GitHubRepositoryStatisticsService(gitHubGraphQLClient);
    }

    @Test
    public void fetchRepositoryStatistics_success_setsDto() throws Exception {
        GitHubTotalCountDTO totalCountDTO = new GitHubTotalCountDTO(1);

        List<GitHubRefsDTO.GitHubBranchNodeDTO> nodes = List.of(new GitHubRefsDTO.GitHubBranchNodeDTO(
                "branch1", new GitHubRefsDTO.GitHubTargetDTO(new GitHubTotalCountDTO(1))));
        GitHubRefsDTO refsDTO = new GitHubRefsDTO(nodes);

        GitHubRepositoryStatisticsDTO dto = new GitHubRepositoryStatisticsDTO(totalCountDTO, totalCountDTO, refsDTO);

        when(gitHubGraphQLClient.getRepositoryStatistics()).thenReturn(dto);

        service.fetchRepositoryStatistics();

        assertSame(dto, service.getGitHubRepositoryStatisticsDTO());
        verify(gitHubGraphQLClient, times(1)).getRepositoryStatistics();
    }

    @Test
    public void fetchRepositoryStatistics_whenGitHubClientThrows_setsDtoNullAndThrows() throws Exception {
        when(gitHubGraphQLClient.getRepositoryStatistics()).thenThrow(new GitHubGraphQLClientException("fail", null));

        assertThrows(GitHubGraphQLClientException.class, service::fetchRepositoryStatistics);
        assertNull(service.getGitHubRepositoryStatisticsDTO());
        verify(gitHubGraphQLClient).getRepositoryStatistics();
    }

    @Test
    public void getGitHubRepositoryStatisticsDTO_initiallyNull() {
        assertNull(service.getGitHubRepositoryStatisticsDTO());
    }
}
