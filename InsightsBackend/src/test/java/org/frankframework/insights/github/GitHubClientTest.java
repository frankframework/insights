package org.frankframework.insights.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.frankframework.insights.branch.BranchDTO;
import org.frankframework.insights.common.client.graphql.GraphQLConnectionDTO;
import org.frankframework.insights.common.client.graphql.GraphQLNodeDTO;
import org.frankframework.insights.common.client.graphql.GraphQLPageInfoDTO;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.label.LabelDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.client.HttpGraphQlClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class GitHubClientTest {

    @Mock
    private GitHubProperties gitHubProperties;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpGraphQlClient httpGraphQlClient;

    private GitHubClient gitHubClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private class TestableGitHubClient extends GitHubClient {
        public TestableGitHubClient(GitHubProperties props, ObjectMapper mapper) {
            super(props, mapper);
        }

        @Override
        protected HttpGraphQlClient getGraphQlClient() {
            return httpGraphQlClient;
        }
    }

    @BeforeEach
    public void setUp() {
        when(gitHubProperties.getUrl()).thenReturn("https://api.github.com/graphql");
        when(gitHubProperties.getSecret()).thenReturn("test-secret-token");
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper);
    }

    @Test
    public void getRepositoryStatistics_Success_ReturnsStatistics() throws GitHubClientException {
        GitHubRepositoryStatisticsDTO stats = new GitHubRepositoryStatisticsDTO(null, null, null);
        when(httpGraphQlClient
                        .documentName(GitHubQueryConstants.REPOSITORY_STATISTICS.getDocumentName())
                        .variables(anyMap())
                        .retrieve(GitHubQueryConstants.REPOSITORY_STATISTICS.getRetrievePath())
                        .toEntity(GitHubRepositoryStatisticsDTO.class))
                .thenReturn(Mono.just(stats));

        GitHubRepositoryStatisticsDTO result = gitHubClient.getRepositoryStatistics();

        assertThat(result).isEqualTo(stats);
    }

    @Test
    public void getRepositoryStatistics_Failure_ThrowsGitHubClientException() {
        RuntimeException apiError = new RuntimeException("API Error");
        when(httpGraphQlClient
                        .documentName(GitHubQueryConstants.REPOSITORY_STATISTICS.getDocumentName())
                        .variables(anyMap())
                        .retrieve(GitHubQueryConstants.REPOSITORY_STATISTICS.getRetrievePath())
                        .toEntity(any(Class.class)))
                .thenReturn(Mono.error(apiError));

        assertThatThrownBy(() -> gitHubClient.getRepositoryStatistics())
                .isInstanceOf(GitHubClientException.class)
                .hasMessage("Failed to fetch repository statistics from GitHub.")
                .hasCauseInstanceOf(Exception.class);
    }

    @Test
    public void getLabels_Success_ReturnsLabels() throws GitHubClientException {
        Map<String, Object> labelNodeMap =
                Map.of("id", "L_1", "name", "bug", "description", "Bug report", "color", "d73a4a");
        LabelDTO expectedLabel = new LabelDTO("L_1", "bug", "Bug report", "d73a4a");

        GraphQLNodeDTO<Map<String, Object>> node = new GraphQLNodeDTO<>(labelNodeMap);
        GraphQLPageInfoDTO pageInfo = new GraphQLPageInfoDTO(false, null);
        GraphQLConnectionDTO<Map<String, Object>> connection = new GraphQLConnectionDTO<>(List.of(node), pageInfo);

        when(httpGraphQlClient
                        .documentName(GitHubQueryConstants.LABELS.getDocumentName())
                        .variables(anyMap())
                        .retrieve(GitHubQueryConstants.LABELS.getRetrievePath())
                        .toEntity(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(connection));

        Set<LabelDTO> labels = gitHubClient.getLabels();

        assertThat(labels).hasSize(1);
        assertThat(labels.iterator().next()).usingRecursiveComparison().isEqualTo(expectedLabel);
    }

    @Test
    public void getLabels_Empty_ReturnsEmptySet() throws GitHubClientException {
        GraphQLPageInfoDTO pageInfo = new GraphQLPageInfoDTO(false, null);
        GraphQLConnectionDTO<Map<String, Object>> connection =
                new GraphQLConnectionDTO<>(Collections.emptyList(), pageInfo);

        when(httpGraphQlClient
                        .documentName(GitHubQueryConstants.LABELS.getDocumentName())
                        .variables(anyMap())
                        .retrieve(GitHubQueryConstants.LABELS.getRetrievePath())
                        .toEntity(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(connection));

        Set<LabelDTO> labels = gitHubClient.getLabels();

        assertThat(labels).isEmpty();
    }

    @Test
    public void getBranches_Success_ReturnsBranches() throws GitHubClientException {
        Map<String, Object> branchNodeMap = Map.of("id", "B_1", "name", "main");
        BranchDTO expectedBranch = new BranchDTO("B_1", "main");

        GraphQLNodeDTO<Map<String, Object>> node = new GraphQLNodeDTO<>(branchNodeMap);
        GraphQLPageInfoDTO pageInfo = new GraphQLPageInfoDTO(false, null);
        GraphQLConnectionDTO<Map<String, Object>> connection = new GraphQLConnectionDTO<>(List.of(node), pageInfo);

        when(httpGraphQlClient
                        .documentName(GitHubQueryConstants.BRANCHES.getDocumentName())
                        .variables(anyMap())
                        .retrieve(GitHubQueryConstants.BRANCHES.getRetrievePath())
                        .toEntity(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(connection));

        Set<BranchDTO> branches = gitHubClient.getBranches();

        assertThat(branches).hasSize(1);
        assertThat(branches.iterator().next()).usingRecursiveComparison().isEqualTo(expectedBranch);
    }

    @Test
    public void getBranchPullRequests_Failure_ThrowsGitHubClientException() {
        String branchName = "feature-branch";
        RuntimeException apiError = new RuntimeException("API Error");
        when(httpGraphQlClient
                        .documentName(GitHubQueryConstants.BRANCH_PULLS.getDocumentName())
                        .variables(anyMap())
                        .retrieve(GitHubQueryConstants.BRANCH_PULLS.getRetrievePath())
                        .toEntity(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(apiError));

        assertThatThrownBy(() -> gitHubClient.getBranchPullRequests(branchName))
                .isInstanceOf(GitHubClientException.class)
                .hasMessage("Failed to fetch pull requests for branch 'feature-branch' from GitHub.")
                .hasCauseInstanceOf(Exception.class);
    }
}
