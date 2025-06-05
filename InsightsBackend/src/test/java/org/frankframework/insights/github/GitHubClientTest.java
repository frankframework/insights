package org.frankframework.insights.github;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.frankframework.insights.branch.BranchDTO;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.issue.IssueDTO;
import org.frankframework.insights.issuePriority.IssuePriorityDTO;
import org.frankframework.insights.issuetype.IssueTypeDTO;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;
import org.frankframework.insights.pullrequest.PullRequestDTO;
import org.frankframework.insights.release.ReleaseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.client.HttpGraphQlClient;
import reactor.core.publisher.Mono;

public class GitHubClientTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GitHubProperties gitHubProperties;

    @Mock
    private HttpGraphQlClient httpGraphQlClient;

    private GitHubClient gitHubClient;

    private static class TestableGitHubClient extends GitHubClient {
        private final HttpGraphQlClient testClient;

        public TestableGitHubClient(GitHubProperties props, ObjectMapper om, HttpGraphQlClient client) {
            super(props, om);
            this.testClient = client;
        }

        @Override
        protected HttpGraphQlClient getGraphQlClient() {
            return testClient;
        }
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(gitHubProperties.getUrl()).thenReturn("https://api.github.com");
        when(gitHubProperties.getSecret()).thenReturn("secret");
    }

    @Test
    public void getLabels_success() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        LabelDTO label1 = new LabelDTO();
        LabelDTO label2 = new LabelDTO();
        Set<LabelDTO> labels = Set.of(label1, label2);
        doReturn(labels).when(gitHubClient).getEntities(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        Set<LabelDTO> result = gitHubClient.getLabels();
        assertEquals(labels, result);
    }

    @Test
    public void getLabels_empty() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(Collections.emptySet())
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        assertTrue(gitHubClient.getLabels().isEmpty());
    }

    @Test
    public void getLabels_nullReturned() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(null).when(gitHubClient).getEntities(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        assertThrows(NullPointerException.class, () -> gitHubClient.getLabels());
    }

    @Test
    public void getLabels_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getLabels());
    }

    @Test
    public void getMilestones_success() throws Exception {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        MilestoneDTO m1 = new MilestoneDTO("id", 1, null, GitHubPropertyState.OPEN);
        Set<MilestoneDTO> milestones = Set.of(m1);
        doReturn(milestones)
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.MILESTONES), anyMap(), eq(MilestoneDTO.class));
        assertEquals(milestones, gitHubClient.getMilestones());
    }

    @Test
    public void getMilestones_empty() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(Collections.emptySet())
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.MILESTONES), anyMap(), eq(MilestoneDTO.class));
        assertTrue(gitHubClient.getMilestones().isEmpty());
    }

    @Test
    public void getMilestones_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.MILESTONES), anyMap(), eq(MilestoneDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getMilestones());
    }

    @Test
    public void getIssueTypes_success() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        IssueTypeDTO t1 = new IssueTypeDTO();
        Set<IssueTypeDTO> set = Set.of(t1);
        doReturn(set)
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.ISSUE_TYPES), anyMap(), eq(IssueTypeDTO.class));
        assertEquals(set, gitHubClient.getIssueTypes());
    }

    @Test
    public void getIssueTypes_empty() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(Collections.emptySet())
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.ISSUE_TYPES), anyMap(), eq(IssueTypeDTO.class));
        assertTrue(gitHubClient.getIssueTypes().isEmpty());
    }

    @Test
    public void getIssueTypes_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.ISSUE_TYPES), anyMap(), eq(IssueTypeDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getIssueTypes());
    }

    @Test
    public void getIssuePriorities_success() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        String projectId = "pid";
        GitHubPrioritySingleSelectDTO.SingleSelectObject<IssuePriorityDTO> obj =
                new GitHubPrioritySingleSelectDTO.SingleSelectObject<>();
        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject<IssuePriorityDTO>> set = Set.of(obj);

        doReturn(set)
                .when(gitHubClient)
                .getNodes(
                        eq(GitHubQueryConstants.ISSUE_PRIORITIES),
                        anyMap(),
                        any(ParameterizedTypeReference.class),
                        eq(GitHubPrioritySingleSelectDTO.SingleSelectObject.class));
        assertEquals(set, gitHubClient.getIssuePriorities(projectId));
    }

    @Test
    public void getIssuePriorities_empty() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        String projectId = "pid";
        doReturn(Collections.emptySet())
                .when(gitHubClient)
                .getNodes(
                        eq(GitHubQueryConstants.ISSUE_PRIORITIES),
                        anyMap(),
                        any(ParameterizedTypeReference.class),
                        eq(GitHubPrioritySingleSelectDTO.SingleSelectObject.class));
        assertTrue(gitHubClient.getIssuePriorities(projectId).isEmpty());
    }

    @Test
    public void getIssuePriorities_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        String projectId = "pid";
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .getNodes(
                        eq(GitHubQueryConstants.ISSUE_PRIORITIES),
                        anyMap(),
                        any(ParameterizedTypeReference.class),
                        eq(GitHubPrioritySingleSelectDTO.SingleSelectObject.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getIssuePriorities(projectId));
    }

    @Test
    public void getBranches_success() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        BranchDTO branch = new BranchDTO();
        branch.setName("main");
        Set<BranchDTO> set = Set.of(branch);
        doReturn(set).when(gitHubClient).getEntities(eq(GitHubQueryConstants.BRANCHES), anyMap(), eq(BranchDTO.class));
        assertEquals(set, gitHubClient.getBranches());
    }

    @Test
    public void getBranches_empty() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(Collections.emptySet())
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.BRANCHES), anyMap(), eq(BranchDTO.class));
        assertTrue(gitHubClient.getBranches().isEmpty());
    }

    @Test
    public void getBranches_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.BRANCHES), anyMap(), eq(BranchDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getBranches());
    }

    @Test
    public void getIssues_success() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        IssueDTO dto = new IssueDTO(
                "id",
                1,
                "Test Issue",
                GitHubPropertyState.OPEN,
                null,
                "http://example.com",
                new GitHubEdgesDTO<>(),
                null,
                null,
                new GitHubEdgesDTO<>(),
                new GitHubEdgesDTO<>());
        Set<IssueDTO> set = Set.of(dto);
        doReturn(set).when(gitHubClient).getEntities(eq(GitHubQueryConstants.ISSUES), anyMap(), eq(IssueDTO.class));
        assertEquals(set, gitHubClient.getIssues());
    }

    @Test
    public void getIssues_empty() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(Collections.emptySet())
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.ISSUES), anyMap(), eq(IssueDTO.class));
        assertTrue(gitHubClient.getIssues().isEmpty());
    }

    @Test
    public void getIssues_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.ISSUES), anyMap(), eq(IssueDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getIssues());
    }

    @Test
    public void getBranchPullRequests_success() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        PullRequestDTO pr =
                new PullRequestDTO("id", 1, "Test PR", GitHubPropertyState.OPEN.name(), null, null, null, null);
        Set<PullRequestDTO> prs = Set.of(pr);
        doReturn(prs)
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.BRANCH_PULLS), anyMap(), eq(PullRequestDTO.class));
        assertEquals(prs, gitHubClient.getBranchPullRequests("main"));
    }

    @Test
    public void getBranchPullRequests_empty() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(Collections.emptySet())
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.BRANCH_PULLS), anyMap(), eq(PullRequestDTO.class));
        assertTrue(gitHubClient.getBranchPullRequests("main").isEmpty());
    }

    @Test
    public void getBranchPullRequests_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.BRANCH_PULLS), anyMap(), eq(PullRequestDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getBranchPullRequests("main"));
    }

    @Test
    public void getReleases_success() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        ReleaseDTO dto = new ReleaseDTO();
        Set<ReleaseDTO> set = Set.of(dto);
        doReturn(set).when(gitHubClient).getEntities(eq(GitHubQueryConstants.RELEASES), anyMap(), eq(ReleaseDTO.class));
        assertEquals(set, gitHubClient.getReleases());
    }

    @Test
    public void getReleases_empty() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(Collections.emptySet())
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.RELEASES), anyMap(), eq(ReleaseDTO.class));
        assertTrue(gitHubClient.getReleases().isEmpty());
    }

    @Test
    public void getReleases_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .getEntities(eq(GitHubQueryConstants.RELEASES), anyMap(), eq(ReleaseDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getReleases());
    }

    @Test
    public void getRepositoryStatistics_success() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        GitHubRepositoryStatisticsDTO stats = new GitHubRepositoryStatisticsDTO(null, null, null, null, null, null);
        doReturn(stats)
                .when(gitHubClient)
                .fetchSingleEntity(
                        eq(GitHubQueryConstants.REPOSITORY_STATISTICS),
                        anyMap(),
                        eq(GitHubRepositoryStatisticsDTO.class));
        assertEquals(stats, gitHubClient.getRepositoryStatistics());
    }

    @Test
    public void getRepositoryStatistics_exception() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .fetchSingleEntity(
                        eq(GitHubQueryConstants.REPOSITORY_STATISTICS),
                        anyMap(),
                        eq(GitHubRepositoryStatisticsDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getRepositoryStatistics());
    }

    @Test
    public void getEntities_handlesNullEdgeCollection() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(null).when(gitHubClient).getEntities(eq(GitHubQueryConstants.BRANCHES), anyMap(), eq(BranchDTO.class));
        assertThrows(NullPointerException.class, () -> gitHubClient.getBranches());
    }

    @Test
    public void getEntities_handlesNullQueryVariables() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        BranchDTO branch = new BranchDTO();
        Set<BranchDTO> set = Set.of(branch);
        doReturn(set).when(gitHubClient).getEntities(eq(GitHubQueryConstants.BRANCHES), isNull(), eq(BranchDTO.class));
    }

    @Test
    public void getNodes_handlesNullResult() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(null).when(gitHubClient).getNodes(eq(GitHubQueryConstants.ISSUE_PRIORITIES), anyMap(), any(), any());
        assertThrows(NullPointerException.class, () -> gitHubClient.getIssuePriorities("pid"));
    }

    @Test
    public void fetchSingleEntity_nullResponse() throws GitHubClientException {
        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
        doReturn(null)
                .when(gitHubClient)
                .fetchSingleEntity(
                        eq(GitHubQueryConstants.REPOSITORY_STATISTICS),
                        anyMap(),
                        eq(GitHubRepositoryStatisticsDTO.class));
        assertNull(gitHubClient.fetchSingleEntity(
                GitHubQueryConstants.REPOSITORY_STATISTICS, new HashMap<>(), GitHubRepositoryStatisticsDTO.class));
    }

    @Test
    public void getEntities_success() throws GitHubClientException {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        LabelDTO label = new LabelDTO();
        GitHubPaginationDTO.Edge<LabelDTO> edge = new GitHubPaginationDTO.Edge<>();
        edge.node = label;
        GitHubPaginationDTO<LabelDTO> dto = new GitHubPaginationDTO<>();
        dto.edges = List.of(edge);
        dto.pageInfo = new GitHubPageInfo();
        dto.pageInfo.hasNextPage = false;

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dto));
        when(objectMapper.convertValue(label, LabelDTO.class)).thenReturn(label);

        Set<LabelDTO> result = gitHubClient.getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
        assertEquals(1, result.size());
        assertTrue(result.contains(label));
    }

    @Test
    public void getEntities_emptyEdges() throws GitHubClientException {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        GitHubPaginationDTO<LabelDTO> dto = new GitHubPaginationDTO<>();
        dto.edges = new ArrayList<>();
        dto.pageInfo = new GitHubPageInfo();
        dto.pageInfo.hasNextPage = false;

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dto));

        Set<LabelDTO> result = gitHubClient.getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getEntities_nullEdges() throws GitHubClientException {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        GitHubPaginationDTO<LabelDTO> dto = new GitHubPaginationDTO<>();
        dto.edges = null;
        dto.pageInfo = new GitHubPageInfo();
        dto.pageInfo.hasNextPage = false;

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dto));
        when(objectMapper.convertValue(null, LabelDTO.class)).thenReturn(null);

        Set<LabelDTO> result = gitHubClient.getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getEntities_graphQLThrowsException() {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenThrow(RuntimeException.class);

        assertThrows(
                GitHubClientException.class,
                () -> gitHubClient.getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class));
    }

    @Test
    public void getNodes_success() throws GitHubClientException {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        GitHubPrioritySingleSelectDTO.SingleSelectObject<IssuePriorityDTO> obj =
                new GitHubPrioritySingleSelectDTO.SingleSelectObject<>();
        GitHubPrioritySingleSelectDTO<IssuePriorityDTO> dto = new GitHubPrioritySingleSelectDTO<>();
        dto.nodes = List.of(obj);
        dto.pageInfo = new GitHubPageInfo();
        dto.pageInfo.hasNextPage = false;

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dto));

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject<IssuePriorityDTO>> result = gitHubClient.getNodes(
                GitHubQueryConstants.ISSUE_PRIORITIES,
                new HashMap<>(),
                new ParameterizedTypeReference<>() {},
                GitHubPrioritySingleSelectDTO.SingleSelectObject.class);
        assertEquals(1, result.size());
    }

    @Test
    public void getNodes_emptyNodes() throws GitHubClientException {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        GitHubPrioritySingleSelectDTO<IssuePriorityDTO> dto = new GitHubPrioritySingleSelectDTO<>();
        dto.nodes = new ArrayList<>();
        dto.pageInfo = new GitHubPageInfo();
        dto.pageInfo.hasNextPage = false;

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dto));

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject<IssuePriorityDTO>> result = gitHubClient.getNodes(
                GitHubQueryConstants.ISSUE_PRIORITIES,
                new HashMap<>(),
                new ParameterizedTypeReference<>() {},
                GitHubPrioritySingleSelectDTO.SingleSelectObject.class);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getNodes_nullResponse() throws Exception {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.empty());

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject<IssuePriorityDTO>> result = gitHubClient.getNodes(
                GitHubQueryConstants.ISSUE_PRIORITIES,
                new HashMap<>(),
                new ParameterizedTypeReference<>() {},
                GitHubPrioritySingleSelectDTO.SingleSelectObject.class);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getNodes_graphQLThrowsException() {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenThrow(RuntimeException.class);

        assertThrows(
                GitHubClientException.class,
                () -> gitHubClient.getNodes(
                        GitHubQueryConstants.ISSUE_PRIORITIES,
                        new HashMap<>(),
                        new ParameterizedTypeReference<GitHubPrioritySingleSelectDTO<IssuePriorityDTO>>() {},
                        GitHubPrioritySingleSelectDTO.SingleSelectObject.class));
    }

    @Test
    public void fetchSingleEntity_success() throws GitHubClientException {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        GitHubRepositoryStatisticsDTO statistics = new GitHubRepositoryStatisticsDTO(
                new GitHubTotalCountDTO(10),
                new GitHubTotalCountDTO(5),
                new GitHubTotalCountDTO(3),
                new GitHubRefsDTO(List.of(new GitHubRefsDTO.GitHubBranchNodeDTO(
                        "main", new GitHubRefsDTO.GitHubTargetDTO(new GitHubTotalCountDTO(2))))),
                new GitHubTotalCountDTO(1),
                new GitHubTotalCountDTO(0));
        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(Class.class))).thenReturn(Mono.just(statistics));
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(statistics));

        GitHubRepositoryStatisticsDTO result = gitHubClient.fetchSingleEntity(
                GitHubQueryConstants.REPOSITORY_STATISTICS, new HashMap<>(), GitHubRepositoryStatisticsDTO.class);

        assertEquals(statistics, result);
    }

    @Test
    public void fetchSingleEntity_graphQLThrowsException() {
        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);

        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);

        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenThrow(RuntimeException.class);

        assertThrows(
                GitHubClientException.class,
                () -> gitHubClient.fetchSingleEntity(
                        GitHubQueryConstants.REPOSITORY_STATISTICS,
                        new HashMap<>(),
                        GitHubRepositoryStatisticsDTO.class));
    }
}
