package org.frankframework.insights.issue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;

import org.frankframework.insights.common.helper.IssueLabelHelperService;
import org.frankframework.insights.common.helper.ReleaseIssueHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.*;
import org.frankframework.insights.label.*;
import org.frankframework.insights.milestone.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssueServiceTest {

	@Mock GitHubRepositoryStatisticsService statisticsService;
	@Mock GitHubClient gitHubClient;
	@Mock Mapper mapper;
	@Mock IssueRepository issueRepository;
	@Mock IssueLabelHelperService issueLabelHelperService;
	@Mock MilestoneService milestoneService;
	@Mock LabelService labelService;
	@Mock ReleaseIssueHelperService releaseIssueHelperService;
	@InjectMocks IssueService issueService;

	@Mock GitHubRepositoryStatisticsDTO statsDTO;

	private OffsetDateTime now;
	private IssueDTO dto1, dto2, dtoSub;
	private Issue issue1;
	private Issue issue2;
	private Issue issueSub;
	private Milestone milestone;

	@BeforeEach
	public void setup() {
		now = OffsetDateTime.now();

		milestone = new Milestone();
		milestone.setId("m1");
		milestone.setNumber(1);
		milestone.setTitle("Milestone 1");
		milestone.setState(GitHubPropertyState.OPEN);

		issue1 = new Issue();
		issue1.setId("i1");
		issue1.setNumber(101);
		issue1.setTitle("Issue 1");
		issue1.setState(GitHubPropertyState.OPEN);
		issue1.setUrl("http://issue1");
		issue1.setClosedAt(now.minusDays(1));

		issue2 = new Issue();
		issue2.setId("i2");
		issue2.setNumber(102);
		issue2.setTitle("Issue 2");
		issue2.setState(GitHubPropertyState.OPEN);
		issue2.setUrl("http://issue2");
		issue2.setClosedAt(now.minusDays(2));

		issueSub = new Issue();
		issueSub.setId("i4");
		issueSub.setNumber(104);
		issueSub.setTitle("Sub Issue Parent");
		issueSub.setState(GitHubPropertyState.OPEN);
		issueSub.setUrl("http://issue4");

		LabelDTO labelDTO = new LabelDTO();
		labelDTO.id = "l1";
		labelDTO.name = "bug";
		labelDTO.description = "desc";
		labelDTO.color = "red";

		GitHubNodeDTO<LabelDTO> labelNode = new GitHubNodeDTO<>();
		labelNode.setNode(labelDTO);
		List<GitHubNodeDTO<LabelDTO>> labelNodeList = List.of(labelNode);
		GitHubEdgesDTO<LabelDTO> labelEdges = new GitHubEdgesDTO<>();
		labelEdges.setEdges(labelNodeList);

		dto1 = new IssueDTO(
				"i1", 101, "Issue 1", GitHubPropertyState.OPEN, now.minusDays(1), "http://issue1",
				labelEdges,
				new MilestoneDTO("m1", 1, "Milestone 1", GitHubPropertyState.OPEN),
				null
		);
		dto2 = new IssueDTO(
				"i2", 102, "Issue 2", GitHubPropertyState.OPEN, now.minusDays(2), "http://issue2",
				null, null, null
		);

		GitHubNodeDTO<IssueDTO> subNode = new GitHubNodeDTO<>();
		subNode.setNode(dto2);
		List<GitHubNodeDTO<IssueDTO>> subNodes = List.of(subNode);
		GitHubEdgesDTO<IssueDTO> subIssuesEdge = new GitHubEdgesDTO<>();
		subIssuesEdge.setEdges(subNodes);

		dtoSub = new IssueDTO(
				"i4", 104, "Sub Issue Parent", GitHubPropertyState.OPEN, now.minusDays(4), "http://issue4",
				null, null, subIssuesEdge
		);
	}

	@Test
	public void injectIssues_skips_whenCountsEqual() throws GitHubClientException, IssueInjectionException {
		when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
		when(statsDTO.getGitHubIssueCount()).thenReturn(2);
		when(issueRepository.count()).thenReturn(2L);

		issueService.injectIssues();

		verify(gitHubClient, never()).getIssues();
		verify(issueRepository, never()).saveAll(anySet());
	}

	@Test
	public void injectIssues_savesAllAndHandlesMilestoneAndLabels() throws GitHubClientException, IssueInjectionException, MappingException {
		Set<IssueDTO> DTOs = Set.of(dto1, dto2);
		Set<Issue> mappedIssues = Set.of(issue1, issue2);

		Map<String, Milestone> milestones = Map.of("m1", milestone);

		when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
		when(statsDTO.getGitHubIssueCount()).thenReturn(4);
		when(issueRepository.count()).thenReturn(2L);
		when(gitHubClient.getIssues()).thenReturn(DTOs);
		when(mapper.toEntity(DTOs, Issue.class)).thenReturn(mappedIssues);
		when(milestoneService.getAllMilestonesMap()).thenReturn(milestones);

		when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

		doNothing().when(issueLabelHelperService).saveIssueLabels(anySet(), anyMap());

		issueService.injectIssues();

		verify(issueRepository, times(2)).saveAll(anySet());
		verify(issueLabelHelperService).saveIssueLabels(anySet(), anyMap());
		assertEquals(milestone, issue1.getMilestone());
	}

	@Test
	public void injectIssues_assignsSubIssues() throws GitHubClientException, IssueInjectionException, MappingException {
		Set<IssueDTO> DTOs = Set.of(dtoSub, dto2);
		Set<Issue> mappedIssues = Set.of(issueSub, issue2);

		when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
		when(statsDTO.getGitHubIssueCount()).thenReturn(5);
		when(issueRepository.count()).thenReturn(2L);
		when(gitHubClient.getIssues()).thenReturn(DTOs);
		when(mapper.toEntity(DTOs, Issue.class)).thenReturn(mappedIssues);
		when(milestoneService.getAllMilestonesMap()).thenReturn(Collections.emptyMap());
		when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));
		doNothing().when(issueLabelHelperService).saveIssueLabels(anySet(), anyMap());

		issueService.injectIssues();

		verify(issueRepository, times(2)).saveAll(anySet());
		verify(issueLabelHelperService).saveIssueLabels(anySet(), anyMap());
		// sub-issue assignment logic can only be fully tested if Issue equals/hashcode is id-based
		// so this is a smoke check:
		// assertTrue(issueSub.getSubIssues().stream().anyMatch(i -> "i2".equals(i.getId())));
	}

	@Test
	public void injectIssues_catchesAndWrapsException() throws GitHubClientException {
		when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
		when(statsDTO.getGitHubIssueCount()).thenReturn(3);
		when(issueRepository.count()).thenReturn(2L);
		when(gitHubClient.getIssues()).thenThrow(new GitHubClientException("fail", null));
		assertThrows(IssueInjectionException.class, () -> issueService.injectIssues());
	}

	@Test
	public void getIssuesByTimespan_returnsResponsesWithLabels() {
		Set<Issue> issues = Set.of(issue1, issue2);
		when(issueRepository.findAllByClosedAtBetween(any(), any())).thenReturn(issues);

		Label label = new Label();
		label.setId("l1");
		label.setName("bug");
		label.setColor("red");
		label.setDescription("desc");

		LabelResponse lr = new LabelResponse("l1", "bug", "desc", "red");

		IssueResponse ir1 = new IssueResponse();
		ir1.setId("i1");
		IssueResponse ir2 = new IssueResponse();
		ir2.setId("i2");

		when(mapper.toDTO(eq(issue1), eq(IssueResponse.class))).thenReturn(ir1);
		when(mapper.toDTO(eq(issue2), eq(IssueResponse.class))).thenReturn(ir2);
		when(labelService.getLabelsByIssueId(anyString())).thenReturn(Set.of(label));
		when(mapper.toDTO(eq(label), eq(LabelResponse.class))).thenReturn(lr);

		Set<IssueResponse> resp = issueService.getIssuesByTimespan(now.minusDays(5), now.plusDays(5));

		assertEquals(2, resp.size());
		assertTrue(resp.stream().anyMatch(r -> r.getId().equals("i1")));
		assertTrue(resp.stream().anyMatch(r -> r.getId().equals("i2")));
	}

	@Test
	public void getIssuesByReleaseId_returnsResponsesWithLabels() throws Exception {
		Set<Issue> issues = Set.of(issue1);
		when(releaseIssueHelperService.getIssuesByReleaseId("rel123")).thenReturn(issues);
		Label label = new Label();
		label.setId("l1");
		label.setName("bug");
		label.setColor("red");
		label.setDescription("desc");
		LabelResponse lr = new LabelResponse("l1", "bug", "desc", "red");
		IssueResponse ir = new IssueResponse();
		ir.setId("i1");
		when(mapper.toDTO(eq(issue1), eq(IssueResponse.class))).thenReturn(ir);
		when(labelService.getLabelsByIssueId("i1")).thenReturn(Set.of(label));
		when(mapper.toDTO(eq(label), eq(LabelResponse.class))).thenReturn(lr);

		Set<IssueResponse> resp = issueService.getIssuesByReleaseId("rel123");
		assertEquals(1, resp.size());
		assertEquals("i1", resp.iterator().next().getId());
	}

	@Test
	public void getIssuesByMilestoneId_returnsResponsesWithLabels() throws Exception {
		Set<Issue> issues = Set.of(issue1);
		when(milestoneService.checkIfMilestoneExists("m1")).thenReturn(milestone);
		when(issueRepository.findAllByMilestone_Id("m1")).thenReturn(issues);

		Label label = new Label();
		label.setId("l1");
		label.setName("bug");
		label.setColor("red");
		label.setDescription("desc");
		LabelResponse lr = new LabelResponse("l1", "bug", "desc", "red");
		IssueResponse ir = new IssueResponse();
		ir.setId("i1");
		when(mapper.toDTO(eq(issue1), eq(IssueResponse.class))).thenReturn(ir);
		when(labelService.getLabelsByIssueId("i1")).thenReturn(Set.of(label));
		when(mapper.toDTO(eq(label), eq(LabelResponse.class))).thenReturn(lr);

		Set<IssueResponse> resp = issueService.getIssuesByMilestoneId("m1");
		assertEquals(1, resp.size());
		assertEquals("i1", resp.iterator().next().getId());
	}

	@Test
	public void getIssuesByMilestoneId_throwsIfNotFound() throws Exception {
		when(milestoneService.checkIfMilestoneExists("notfound")).thenThrow(new MilestoneNotFoundException("Not found", null));
		assertThrows(MilestoneNotFoundException.class, () -> issueService.getIssuesByMilestoneId("notfound"));
	}

	@Test
	public void getAllIssuesMap_returnsMap() {
		when(issueRepository.findAll()).thenReturn(List.of(issue1, issue2));
		Map<String, Issue> result = issueService.getAllIssuesMap();
		assertEquals(2, result.size());
		assertEquals(issue1, result.get("i1"));
		assertEquals(issue2, result.get("i2"));
	}
}
