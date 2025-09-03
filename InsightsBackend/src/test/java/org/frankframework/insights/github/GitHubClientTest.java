// package org.frankframework.insights.github;
//
// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import java.util.*;
// import org.frankframework.insights.branch.BranchDTO;
// import org.frankframework.insights.common.client.graphql.GraphQLConnectionDTO;
// import org.frankframework.insights.common.client.graphql.GraphQLNodeDTO;
// import org.frankframework.insights.common.configuration.properties.GitHubProperties;
// import org.frankframework.insights.issue.IssueDTO;
// import org.frankframework.insights.issuePriority.IssuePriorityDTO;
// import org.frankframework.insights.issuetype.IssueTypeDTO;
// import org.frankframework.insights.label.LabelDTO;
// import org.frankframework.insights.milestone.MilestoneDTO;
// import org.frankframework.insights.pullrequest.PullRequestDTO;
// import org.frankframework.insights.release.ReleaseDTO;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.*;
// import org.springframework.core.ParameterizedTypeReference;
// import org.springframework.graphql.client.HttpGraphQlClient;
// import reactor.core.publisher.Mono;
//
// public class GitHubClientTest {
//
//    @Mock
//    private ObjectMapper objectMapper;
//
//    @Mock
//    private GitHubProperties gitHubProperties;
//
//    @Mock
//    private HttpGraphQlClient httpGraphQlClient;
//
//    private GitHubClient gitHubClient;
//
//    private static class TestableGitHubClient extends GitHubClient {
//        private final HttpGraphQlClient testClient;
//
//        public TestableGitHubClient(GitHubProperties props, ObjectMapper om, HttpGraphQlClient client) {
//            super(props, om);
//            this.testClient = client;
//        }
//
//        @Override
//        protected HttpGraphQlClient getGraphQlClient() {
//            return testClient;
//        }
//    }
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//        when(gitHubProperties.getUrl()).thenReturn("https://api.github.com");
//        when(gitHubProperties.getSecret()).thenReturn("secret");
//    }
//
//    @Test
//    public void getLabels_success() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        LabelDTO label1 = new LabelDTO("id1", "Label 1", "Label 1 description", "red");
//        LabelDTO label2 = new LabelDTO("id2", "Label 2", "Label 2 description", "blue");
//        Set<LabelDTO> labels = Set.of(label1, label2);
//        doReturn(labels).when(gitHubClient).getEntities(eq(GitHubQueryConstants.LABELS), anyMap(),
// eq(LabelDTO.class));
//        Set<LabelDTO> result = gitHubClient.getLabels();
//        assertEquals(labels, result);
//    }
//
//    @Test
//    public void getLabels_empty() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(Collections.emptySet())
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
//        assertTrue(gitHubClient.getLabels().isEmpty());
//    }
//
//    @Test
//    public void getLabels_nullReturned() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(null).when(gitHubClient).getEntities(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
//        assertThrows(NullPointerException.class, () -> gitHubClient.getLabels());
//    }
//
//    @Test
//    public void getLabels_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getLabels());
//    }
//
//    @Test
//    public void getMilestones_success() throws Exception {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        MilestoneDTO m1 = new MilestoneDTO("id", 1, null, "https//example.com", GitHubPropertyState.OPEN, null, 0, 0);
//        Set<MilestoneDTO> milestones = Set.of(m1);
//        doReturn(milestones)
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.MILESTONES), anyMap(), eq(MilestoneDTO.class));
//        assertEquals(milestones, gitHubClient.getMilestones());
//    }
//
//    @Test
//    public void getMilestones_empty() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(Collections.emptySet())
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.MILESTONES), anyMap(), eq(MilestoneDTO.class));
//        assertTrue(gitHubClient.getMilestones().isEmpty());
//    }
//
//    @Test
//    public void getMilestones_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.MILESTONES), anyMap(), eq(MilestoneDTO.class));
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getMilestones());
//    }
//
//    @Test
//    public void getIssueTypes_success() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        IssueTypeDTO t1 = new IssueTypeDTO("id", "Bug", "A bug in the code", "bug");
//        Set<IssueTypeDTO> set = Set.of(t1);
//        doReturn(set)
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.ISSUE_TYPES), anyMap(), eq(IssueTypeDTO.class));
//        assertEquals(set, gitHubClient.getIssueTypes());
//    }
//
//    @Test
//    public void getIssueTypes_empty() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(Collections.emptySet())
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.ISSUE_TYPES), anyMap(), eq(IssueTypeDTO.class));
//        assertTrue(gitHubClient.getIssueTypes().isEmpty());
//    }
//
//    @Test
//    public void getIssueTypes_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.ISSUE_TYPES), anyMap(), eq(IssueTypeDTO.class));
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getIssueTypes());
//    }
//
//    @Test
//    public void getIssuePriorities_success() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        String projectId = "pid";
//        GitHubPrioritySingleSelectDTO.SingleSelectObject obj =
//                new GitHubPrioritySingleSelectDTO.SingleSelectObject("priority", null);
//        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> set = Set.of(obj);
//
//        doReturn(set)
//                .when(gitHubClient)
//                .getNodes(eq(GitHubQueryConstants.ISSUE_PRIORITIES), anyMap(), any(ParameterizedTypeReference.class));
//
//        assertEquals(set, gitHubClient.getIssuePriorities(projectId));
//    }
//
//    @Test
//    public void getIssuePriorities_empty() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        String projectId = "pid";
//        doReturn(Collections.emptySet())
//                .when(gitHubClient)
//                .getNodes(eq(GitHubQueryConstants.ISSUE_PRIORITIES), anyMap(), any(ParameterizedTypeReference.class));
//
//        assertTrue(gitHubClient.getIssuePriorities(projectId).isEmpty());
//    }
//
//    @Test
//    public void getIssuePriorities_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        String projectId = "pid";
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .getNodes(eq(GitHubQueryConstants.ISSUE_PRIORITIES), anyMap(), any(ParameterizedTypeReference.class));
//
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getIssuePriorities(projectId));
//    }
//
//    @Test
//    public void getBranches_success() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        BranchDTO branch = new BranchDTO("id", "main");
//        Set<BranchDTO> set = Set.of(branch);
//        doReturn(set).when(gitHubClient).getEntities(eq(GitHubQueryConstants.BRANCHES), anyMap(),
// eq(BranchDTO.class));
//        assertEquals(set, gitHubClient.getBranches());
//    }
//
//    @Test
//    public void getBranches_empty() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(Collections.emptySet())
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.BRANCHES), anyMap(), eq(BranchDTO.class));
//        assertTrue(gitHubClient.getBranches().isEmpty());
//    }
//
//    @Test
//    public void getBranches_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.BRANCHES), anyMap(), eq(BranchDTO.class));
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getBranches());
//    }
//
//    @Test
//    public void getIssues_success() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//
//        IssueDTO issue = new IssueDTO(
//                "id",
//                1,
//                "Test Issue",
//                GitHubPropertyState.OPEN,
//                null,
//                "http://example.com",
//                new GitHubEdgesDTO<>(null),
//                null,
//                null,
//                new GitHubEdgesDTO<>(null),
//                new GitHubEdgesDTO<>(null));
//        Set<IssueDTO> issues = Set.of(issue);
//
//        doReturn(issues).when(gitHubClient).getEntities(eq(GitHubQueryConstants.ISSUES), anyMap(),
// eq(IssueDTO.class));
//        assertEquals(issues, gitHubClient.getIssues());
//    }
//
//    @Test
//    public void getIssues_empty() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(Collections.emptySet())
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.ISSUES), anyMap(), eq(IssueDTO.class));
//        assertTrue(gitHubClient.getIssues().isEmpty());
//    }
//
//    @Test
//    public void getIssues_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.ISSUES), anyMap(), eq(IssueDTO.class));
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getIssues());
//    }
//
//    @Test
//    public void getBranchPullRequests_success() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        PullRequestDTO pr =
//                new PullRequestDTO("id", 1, "Test PR", GitHubPropertyState.OPEN.name(), null, null, null, null);
//        Set<PullRequestDTO> prs = Set.of(pr);
//        doReturn(prs)
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.BRANCH_PULLS), anyMap(), eq(PullRequestDTO.class));
//        assertEquals(prs, gitHubClient.getBranchPullRequests("main"));
//    }
//
//    @Test
//    public void getBranchPullRequests_empty() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(Collections.emptySet())
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.BRANCH_PULLS), anyMap(), eq(PullRequestDTO.class));
//        assertTrue(gitHubClient.getBranchPullRequests("main").isEmpty());
//    }
//
//    @Test
//    public void getBranchPullRequests_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.BRANCH_PULLS), anyMap(), eq(PullRequestDTO.class));
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getBranchPullRequests("main"));
//    }
//
//    @Test
//    public void getReleases_success() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        ReleaseDTO dto = new ReleaseDTO("id", "v1.0", "Release 1.0", null);
//        Set<ReleaseDTO> set = Set.of(dto);
//        doReturn(set).when(gitHubClient).getEntities(eq(GitHubQueryConstants.RELEASES), anyMap(),
// eq(ReleaseDTO.class));
//        assertEquals(set, gitHubClient.getReleases());
//    }
//
//    @Test
//    public void getReleases_empty() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(Collections.emptySet())
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.RELEASES), anyMap(), eq(ReleaseDTO.class));
//        assertTrue(gitHubClient.getReleases().isEmpty());
//    }
//
//    @Test
//    public void getReleases_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .getEntities(eq(GitHubQueryConstants.RELEASES), anyMap(), eq(ReleaseDTO.class));
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getReleases());
//    }
//
//    @Test
//    public void getRepositoryStatistics_success() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        GitHubRepositoryStatisticsDTO stats = new GitHubRepositoryStatisticsDTO(null, null, null);
//        doReturn(stats)
//                .when(gitHubClient)
//                .fetchSingleEntity(
//                        eq(GitHubQueryConstants.REPOSITORY_STATISTICS),
//                        anyMap(),
//                        eq(GitHubRepositoryStatisticsDTO.class));
//        assertEquals(stats, gitHubClient.getRepositoryStatistics());
//    }
//
//    @Test
//    public void getRepositoryStatistics_exception() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doThrow(new GitHubClientException("fail", null))
//                .when(gitHubClient)
//                .fetchSingleEntity(
//                        eq(GitHubQueryConstants.REPOSITORY_STATISTICS),
//                        anyMap(),
//                        eq(GitHubRepositoryStatisticsDTO.class));
//        assertThrows(GitHubClientException.class, () -> gitHubClient.getRepositoryStatistics());
//    }
//
//    @Test
//    public void getEntities_handlesNullEdgeCollection() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(null).when(gitHubClient).getEntities(eq(GitHubQueryConstants.BRANCHES), anyMap(),
// eq(BranchDTO.class));
//        assertThrows(NullPointerException.class, () -> gitHubClient.getBranches());
//    }
//
//    @Test
//    public void getEntities_handlesNullQueryVariables() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        BranchDTO branch = new BranchDTO("id", "main");
//        Set<BranchDTO> set = Set.of(branch);
//        doReturn(set).when(gitHubClient).getEntities(eq(GitHubQueryConstants.BRANCHES), isNull(),
// eq(BranchDTO.class));
//    }
//
//    @Test
//    public void getNodes_handlesNullResult() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(null).when(gitHubClient).getNodes(eq(GitHubQueryConstants.ISSUE_PRIORITIES), anyMap(), any());
//        assertThrows(NullPointerException.class, () -> gitHubClient.getIssuePriorities("pid"));
//    }
//
//    @Test
//    public void fetchSingleEntity_nullResponse() throws GitHubClientException {
//        gitHubClient = spy(new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient));
//        doReturn(null)
//                .when(gitHubClient)
//                .fetchSingleEntity(
//                        eq(GitHubQueryConstants.REPOSITORY_STATISTICS),
//                        anyMap(),
//                        eq(GitHubRepositoryStatisticsDTO.class));
//        assertNull(gitHubClient.fetchSingleEntity(
//                GitHubQueryConstants.REPOSITORY_STATISTICS, new HashMap<>(), GitHubRepositoryStatisticsDTO.class));
//    }
//
//    @Test
//    public void getEntities_success() throws GitHubClientException {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        LabelDTO label = new LabelDTO("id", "bug", "A bug label", "red");
//        GraphQLNodeDTO<LabelDTO> nodeDTO = new GraphQLNodeDTO<>(label);
//        List<GraphQLNodeDTO<LabelDTO>> nodeList = List.of(nodeDTO);
//        GraphQLPageInfo pageInfo = new GraphQLPageInfo(false, null);
//        GraphQLConnectionDTO<LabelDTO> dto = new GraphQLConnectionDTO<>(nodeList, pageInfo);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dto));
//        when(objectMapper.convertValue(nodeDTO, LabelDTO.class)).thenReturn(label);
//
//        Set<LabelDTO> result = gitHubClient.getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    public void getEntities_emptyEdges() throws GitHubClientException {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        GraphQLPageInfo pageInfo = new GraphQLPageInfo(false, null);
//        GraphQLConnectionDTO<LabelDTO> dto = new GraphQLConnectionDTO<>(List.of(), pageInfo);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dto));
//
//        Set<LabelDTO> result = gitHubClient.getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    public void getEntities_nullEdges() throws GitHubClientException {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        GraphQLPageInfo pageInfo = new GraphQLPageInfo(false, null);
//        GraphQLConnectionDTO<LabelDTO> dtoObj = new GraphQLConnectionDTO<>(null, pageInfo);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dtoObj));
//        when(objectMapper.convertValue(null, LabelDTO.class)).thenReturn(null);
//
//        Set<LabelDTO> result = gitHubClient.getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    public void getEntities_graphQLThrowsException() {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenThrow(RuntimeException.class);
//
//        assertThrows(
//                GitHubClientException.class,
//                () -> gitHubClient.getEntities(GitHubQueryConstants.LABELS, new HashMap<>(), LabelDTO.class));
//    }
//
//    @Test
//    public void getNodes_success() throws GitHubClientException {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        IssuePriorityDTO dto = new IssuePriorityDTO("id", "name", "color", "desc");
//        GitHubPrioritySingleSelectDTO.SingleSelectObject obj =
//                new GitHubPrioritySingleSelectDTO.SingleSelectObject("priority", List.of(dto));
//        GraphQLPageInfo pageInfo = new GraphQLPageInfo(false, null);
//        GitHubPrioritySingleSelectDTO dtoObj = new GitHubPrioritySingleSelectDTO(List.of(obj), pageInfo);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dtoObj));
//
//        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> result = gitHubClient.getNodes(
//                GitHubQueryConstants.ISSUE_PRIORITIES, new HashMap<>(), new ParameterizedTypeReference<>() {});
//
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    public void getNodes_emptyNodes() throws GitHubClientException {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        GraphQLPageInfo pageInfo = new GraphQLPageInfo(false, null);
//        GitHubPrioritySingleSelectDTO dtoObj = new GitHubPrioritySingleSelectDTO(Collections.emptyList(), pageInfo);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(dtoObj));
//
//        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> result = gitHubClient.getNodes(
//                GitHubQueryConstants.ISSUE_PRIORITIES, new HashMap<>(), new ParameterizedTypeReference<>() {});
//
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    public void getNodes_nullResponse() throws Exception {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.empty());
//
//        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> result = gitHubClient.getNodes(
//                GitHubQueryConstants.ISSUE_PRIORITIES, new HashMap<>(), new ParameterizedTypeReference<>() {});
//
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    public void getNodes_graphQLThrowsException() {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenThrow(RuntimeException.class);
//
//        assertThrows(
//                GitHubClientException.class,
//                () -> gitHubClient.getNodes(
//                        GitHubQueryConstants.ISSUE_PRIORITIES,
//                        new HashMap<>(),
//                        new ParameterizedTypeReference<GitHubPrioritySingleSelectDTO>() {}));
//    }
//
//    @Test
//    public void fetchSingleEntity_success() throws GitHubClientException {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        GitHubRepositoryStatisticsDTO statistics = new GitHubRepositoryStatisticsDTO(
//                new GitHubTotalCountDTO(10),
//                new GitHubTotalCountDTO(5),
//                new GitHubRefsDTO(List.of(new GitHubRefsDTO.GitHubBranchNodeDTO(
//                        "main", new GitHubRefsDTO.GitHubTargetDTO(new GitHubTotalCountDTO(2))))));
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(Class.class))).thenReturn(Mono.just(statistics));
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(statistics));
//
//        GitHubRepositoryStatisticsDTO result = gitHubClient.fetchSingleEntity(
//                GitHubQueryConstants.REPOSITORY_STATISTICS, new HashMap<>(), GitHubRepositoryStatisticsDTO.class);
//
//        assertEquals(statistics, result);
//    }
//
//    @Test
//    public void fetchSingleEntity_graphQLThrowsException() {
//        gitHubClient = new TestableGitHubClient(gitHubProperties, objectMapper, httpGraphQlClient);
//
//        HttpGraphQlClient.RequestSpec reqSpec = mock(HttpGraphQlClient.RequestSpec.class);
//        HttpGraphQlClient.RetrieveSpec retrieveSpec = mock(HttpGraphQlClient.RetrieveSpec.class);
//
//        when(httpGraphQlClient.documentName(anyString())).thenReturn(reqSpec);
//        when(reqSpec.variables(anyMap())).thenReturn(reqSpec);
//        when(reqSpec.retrieve(anyString())).thenReturn(retrieveSpec);
//        when(retrieveSpec.toEntity(any(ParameterizedTypeReference.class))).thenThrow(RuntimeException.class);
//
//        assertThrows(
//                GitHubClientException.class,
//                () -> gitHubClient.fetchSingleEntity(
//                        GitHubQueryConstants.REPOSITORY_STATISTICS,
//                        new HashMap<>(),
//                        GitHubRepositoryStatisticsDTO.class));
//    }
// }
