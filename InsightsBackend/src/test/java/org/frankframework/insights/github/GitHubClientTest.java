package org.frankframework.insights.github;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.frankframework.insights.branch.BranchDTO;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.issue.IssueDTO;
import org.frankframework.insights.issuetype.IssueTypeDTO;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;
import org.frankframework.insights.pullrequest.PullRequestDTO;
import org.frankframework.insights.release.ReleaseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

public class GitHubClientTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GitHubProperties gitHubProperties;

    @Spy
    @InjectMocks
    private GitHubClient gitHubClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(gitHubProperties.getUrl()).thenReturn("https://api.github.com");
        when(gitHubProperties.getSecret()).thenReturn("secret");
        gitHubClient = spy(new GitHubClient(gitHubProperties, objectMapper));
    }

    @Test
    void getRepositoryStatistics_success() throws GitHubClientException {
        GitHubTotalCountDTO totalCountDTO = new GitHubTotalCountDTO(1);

        List<GitHubRefsDTO.GitHubBranchNodeDTO> nodes = List.of(new GitHubRefsDTO.GitHubBranchNodeDTO(
                "branch1", new GitHubRefsDTO.GitHubTargetDTO(new GitHubTotalCountDTO(1))));
        GitHubRefsDTO refsDTO = new GitHubRefsDTO(nodes);

        GitHubRepositoryStatisticsDTO dto =
                new GitHubRepositoryStatisticsDTO(totalCountDTO, totalCountDTO, totalCountDTO, refsDTO, totalCountDTO, totalCountDTO);

        doReturn(dto).when(gitHubClient).fetchSingleEntity(any(), anyMap(), eq(GitHubRepositoryStatisticsDTO.class));
        GitHubRepositoryStatisticsDTO result = gitHubClient.getRepositoryStatistics();

        assertEquals(dto, result);
    }

    @Test
    public void getRepositoryStatistics_shouldWrapException() {
        try {
            doThrow(new GitHubClientException("fail", null))
                    .when(gitHubClient)
                    .fetchSingleEntity(any(), anyMap(), eq(GitHubRepositoryStatisticsDTO.class));
        } catch (Exception ignored) {
        }
        assertThrows(GitHubClientException.class, () -> gitHubClient.getRepositoryStatistics());
    }

    @Test
    public void getLabels_success() throws Exception {
        LabelDTO label = new LabelDTO();
        List<GitHubPaginationDTO.Edge<LabelDTO>> edges = new ArrayList<>();
        GitHubPaginationDTO.Edge<LabelDTO> edge = new GitHubPaginationDTO.Edge<>();
        edge.node = label;
        edges.add(edge);
        GitHubPaginationDTO<LabelDTO> page = new GitHubPaginationDTO<>();
        page.edges = edges;
        page.pageInfo = new GitHubPaginationDTO.PageInfo();
        page.pageInfo.hasNextPage = false;

        doReturn(page)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        when(objectMapper.convertValue(label, LabelDTO.class)).thenReturn(label);

        Set<LabelDTO> result = gitHubClient.getLabels();
        assertEquals(Set.of(label), result);
    }

    @Test
    public void getLabels_emptyResponse() throws Exception {
        GitHubPaginationDTO<LabelDTO> page = new GitHubPaginationDTO<>();
        page.edges = Collections.emptyList();
        page.pageInfo = new GitHubPaginationDTO.PageInfo();
        page.pageInfo.hasNextPage = false;
        doReturn(page)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));

        Set<LabelDTO> result = gitHubClient.getLabels();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getLabels_nullResponse() throws Exception {
        doReturn(null)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        Set<LabelDTO> result = gitHubClient.getLabels();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getLabels_shouldWrapException() throws Exception {
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getLabels());
    }

    @Test
    public void getMilestones_success() throws Exception {
        MilestoneDTO dto = new MilestoneDTO("id", 1, null, GitHubPropertyState.OPEN);
        List<GitHubPaginationDTO.Edge<MilestoneDTO>> edges = new ArrayList<>();
        GitHubPaginationDTO.Edge<MilestoneDTO> edge = new GitHubPaginationDTO.Edge<>();
        edge.node = dto;
        edges.add(edge);
        GitHubPaginationDTO<MilestoneDTO> page = new GitHubPaginationDTO<>();
        page.edges = edges;
        page.pageInfo = new GitHubPaginationDTO.PageInfo();
        page.pageInfo.hasNextPage = false;

        doReturn(page)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.MILESTONES), anyMap(), eq(MilestoneDTO.class));
        when(objectMapper.convertValue(dto, MilestoneDTO.class)).thenReturn(dto);

        Set<MilestoneDTO> result = gitHubClient.getMilestones();
        assertEquals(Set.of(dto), result);
    }

    @Test
    public void getMilestones_shouldWrapException() throws Exception {
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.MILESTONES), anyMap(), eq(MilestoneDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getMilestones());
    }

	@Test
	public void getIssueTypes_success() throws Exception {
		IssueTypeDTO issueType = new IssueTypeDTO();
		List<GitHubPaginationDTO.Edge<IssueTypeDTO>> edges = new ArrayList<>();
		GitHubPaginationDTO.Edge<IssueTypeDTO> edge = new GitHubPaginationDTO.Edge<>();
		edge.node = issueType;
		edges.add(edge);
		GitHubPaginationDTO<IssueTypeDTO> page = new GitHubPaginationDTO<>();
		page.edges = edges;
		page.pageInfo = new GitHubPaginationDTO.PageInfo();
		page.pageInfo.hasNextPage = false;

		doReturn(page)
				.when(gitHubClient)
				.fetchEntityPage(eq(GitHubQueryConstants.ISSUE_TYPES), anyMap(), eq(IssueTypeDTO.class));
		when(objectMapper.convertValue(issueType, IssueTypeDTO.class)).thenReturn(issueType);

		Set<LabelDTO> result = gitHubClient.getLabels();
		assertEquals(Set.of(issueType), result);
	}

	@Test
	public void getIssueTypes_shouldWrapException() throws Exception {
		doThrow(new GitHubClientException("fail", null))
				.when(gitHubClient)
				.fetchEntityPage(eq(GitHubQueryConstants.ISSUE_TYPES), anyMap(), eq(IssueTypeDTO.class));
		assertThrows(GitHubClientException.class, () -> gitHubClient.getIssueTypes());
	}

    @Test
    public void getBranches_success() throws Exception {
        BranchDTO dto = new BranchDTO();
        dto.setName("main");
        List<GitHubPaginationDTO.Edge<BranchDTO>> edges = new ArrayList<>();
        GitHubPaginationDTO.Edge<BranchDTO> edge = new GitHubPaginationDTO.Edge<>();
        edge.node = dto;
        edges.add(edge);
        GitHubPaginationDTO<BranchDTO> page = new GitHubPaginationDTO<>();
        page.edges = edges;
        page.pageInfo = new GitHubPaginationDTO.PageInfo();
        page.pageInfo.hasNextPage = false;

        doReturn(page)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.BRANCHES), anyMap(), eq(BranchDTO.class));
        when(objectMapper.convertValue(dto, BranchDTO.class)).thenReturn(dto);

        Set<BranchDTO> result = gitHubClient.getBranches();
        assertEquals(Set.of(dto), result);
    }

    @Test
    public void getBranches_shouldWrapException() throws Exception {
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.BRANCHES), anyMap(), eq(BranchDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getBranches());
    }

    @Test
    public void getIssues_success() throws Exception {
        IssueDTO dto = mock(IssueDTO.class);
        List<GitHubPaginationDTO.Edge<IssueDTO>> edges = new ArrayList<>();
        GitHubPaginationDTO.Edge<IssueDTO> edge = new GitHubPaginationDTO.Edge<>();
        edge.node = dto;
        edges.add(edge);
        GitHubPaginationDTO<IssueDTO> page = new GitHubPaginationDTO<>();
        page.edges = edges;
        page.pageInfo = new GitHubPaginationDTO.PageInfo();
        page.pageInfo.hasNextPage = false;

        doReturn(page)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.ISSUES), anyMap(), eq(IssueDTO.class));
        when(objectMapper.convertValue(dto, IssueDTO.class)).thenReturn(dto);

        Set<IssueDTO> result = gitHubClient.getIssues();
        assertEquals(Set.of(dto), result);
    }

    @Test
    public void getIssues_shouldWrapException() throws Exception {
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.ISSUES), anyMap(), eq(IssueDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getIssues());
    }

    @Test
    public void getBranchPullRequests_success() throws Exception {
        PullRequestDTO dto = mock(PullRequestDTO.class);
        List<GitHubPaginationDTO.Edge<PullRequestDTO>> edges = new ArrayList<>();
        GitHubPaginationDTO.Edge<PullRequestDTO> edge = new GitHubPaginationDTO.Edge<>();
        edge.node = dto;
        edges.add(edge);
        GitHubPaginationDTO<PullRequestDTO> page = new GitHubPaginationDTO<>();
        page.edges = edges;
        page.pageInfo = new GitHubPaginationDTO.PageInfo();
        page.pageInfo.hasNextPage = false;

        doReturn(page)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.BRANCH_PULLS), anyMap(), eq(PullRequestDTO.class));
        when(objectMapper.convertValue(dto, PullRequestDTO.class)).thenReturn(dto);

        Set<PullRequestDTO> result = gitHubClient.getBranchPullRequests("main");
        assertEquals(Set.of(dto), result);
    }

    @Test
    public void getBranchPullRequests_shouldWrapException() throws Exception {
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.BRANCH_PULLS), anyMap(), eq(PullRequestDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getBranches());
    }

    @Test
    public void getReleases_success() throws Exception {
        ReleaseDTO dto = mock(ReleaseDTO.class);
        List<GitHubPaginationDTO.Edge<ReleaseDTO>> edges = new ArrayList<>();
        GitHubPaginationDTO.Edge<ReleaseDTO> edge = new GitHubPaginationDTO.Edge<>();
        edge.node = dto;
        edges.add(edge);
        GitHubPaginationDTO<ReleaseDTO> page = new GitHubPaginationDTO<>();
        page.edges = edges;
        page.pageInfo = new GitHubPaginationDTO.PageInfo();
        page.pageInfo.hasNextPage = false;

        doReturn(page)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.RELEASES), anyMap(), eq(ReleaseDTO.class));
        when(objectMapper.convertValue(dto, ReleaseDTO.class)).thenReturn(dto);

        Set<ReleaseDTO> result = gitHubClient.getReleases();
        assertEquals(Set.of(dto), result);
    }

    @Test
    public void getReleases_shouldWrapException() throws Exception {
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.RELEASES), anyMap(), eq(ReleaseDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getReleases());
    }

    @Test
    public void paginatedQuery_shouldHandleMultiplePages() throws Exception {
        LabelDTO label1 = new LabelDTO();
        LabelDTO label2 = new LabelDTO();

        GitHubPaginationDTO.Edge<LabelDTO> edge1 = new GitHubPaginationDTO.Edge<>();
        edge1.node = label1;
        GitHubPaginationDTO.Edge<LabelDTO> edge2 = new GitHubPaginationDTO.Edge<>();
        edge2.node = label2;

        GitHubPaginationDTO<LabelDTO> page1 = new GitHubPaginationDTO<>();
        page1.edges = List.of(edge1);
        page1.pageInfo = new GitHubPaginationDTO.PageInfo();
        page1.pageInfo.hasNextPage = true;
        page1.pageInfo.endCursor = "cursor1";

        GitHubPaginationDTO<LabelDTO> page2 = new GitHubPaginationDTO<>();
        page2.edges = List.of(edge2);
        page2.pageInfo = new GitHubPaginationDTO.PageInfo();
        page2.pageInfo.hasNextPage = false;
        page2.pageInfo.endCursor = null;

        doReturn(page1)
                .doReturn(page2)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));

        when(objectMapper.convertValue(label1, LabelDTO.class)).thenReturn(label1);
        when(objectMapper.convertValue(label2, LabelDTO.class)).thenReturn(label2);

        Set<LabelDTO> result = gitHubClient.getLabels();
        assertEquals(Set.of(label1, label2), result);
    }

    @Test
    public void paginatedQuery_shouldReturnEmptySetOnNullEdges() throws Exception {
        GitHubPaginationDTO<LabelDTO> page = new GitHubPaginationDTO<>();
        page.edges = null;
        page.pageInfo = new GitHubPaginationDTO.PageInfo();
        page.pageInfo.hasNextPage = false;

        doReturn(page)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));

        Set<LabelDTO> result = gitHubClient.getLabels();
        assertTrue(result.isEmpty());
    }

    @Test
    public void paginatedQuery_shouldReturnEmptySetOnNullResponse() throws Exception {
        doReturn(null)
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        Set<LabelDTO> result = gitHubClient.getLabels();
        assertTrue(result.isEmpty());
    }

    @Test
    public void paginatedQuery_shouldPropagateExceptionFromFetchEntityPage() throws Exception {
        doThrow(new GitHubClientException("fail", null))
                .when(gitHubClient)
                .fetchEntityPage(eq(GitHubQueryConstants.LABELS), anyMap(), eq(LabelDTO.class));
        assertThrows(GitHubClientException.class, () -> gitHubClient.getLabels());
    }
}
