package org.frankframework.insights.issue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.helper.ReleaseIssueHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.*;
import org.frankframework.insights.issuePriority.IssuePriority;
import org.frankframework.insights.issuePriority.IssuePriorityResponse;
import org.frankframework.insights.issuePriority.IssuePriorityService;
import org.frankframework.insights.issuetype.IssueType;
import org.frankframework.insights.issuetype.IssueTypeResponse;
import org.frankframework.insights.issuetype.IssueTypeService;
import org.frankframework.insights.label.*;
import org.frankframework.insights.milestone.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssueServiceTest {

    @Mock
    private GitHubRepositoryStatisticsService statisticsService;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper mapper;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private IssueTypeService issueTypeService;

    @Mock
    private IssuePriorityService issuePriorityService;

    @Mock
    private LabelService labelService;

    @Mock
    private ReleaseIssueHelperService releaseIssueHelperService;

    @InjectMocks
    private IssueService issueService;

    @Mock
    private GitHubRepositoryStatisticsDTO statsDTO;

    private OffsetDateTime now;
    private IssueDTO dto1, dto2, dtoSub;
    private Issue issue1, issue2, issueSub;
    private Milestone milestone;
    private IssueType issueType;

    @BeforeEach
    public void setup() {
        now = OffsetDateTime.now();

        milestone = new Milestone();
        milestone.setId("m1");
        milestone.setNumber(1);
        milestone.setTitle("Milestone 1");
        milestone.setState(GitHubPropertyState.OPEN);

        issueType = new IssueType();
        issueType.setId("it1");
        issueType.setName("ImportantissueType1");
        issueType.setDescription("description1");
        issueType.setColor("purple");

        IssuePriority issuePriority = new IssuePriority();
        issuePriority.setId("ip1");
        issuePriority.setName("High");
        issuePriority.setDescription("High priority issue");
        issuePriority.setColor("red");

        issue1 = new Issue();
        issue1.setId("i1");
        issue1.setNumber(101);
        issue1.setTitle("Issue 1");
        issue1.setState(GitHubPropertyState.OPEN);
        issue1.setUrl("http://issue1");
        issue1.setClosedAt(now.minusDays(1));
        issue1.setIssueType(issueType);
        issue1.setPoints(13.0);
        issue1.setIssuePriority(issuePriority);

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
        issueSub.setParentIssue(issue1);

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

        GitHubEdgesDTO<GitHubProjectItemDTO> emptyProjectItems = new GitHubEdgesDTO<>();
        emptyProjectItems.setEdges(Collections.emptyList());

        dto1 = new IssueDTO(
                "i1",
                101,
                "Issue 1",
                GitHubPropertyState.OPEN,
                now.minusDays(1),
                "http://issue1",
                labelEdges,
                new MilestoneDTO("m1", 1, "Milestone 1", GitHubPropertyState.OPEN),
                null,
                null,
                emptyProjectItems);

        dto2 = new IssueDTO(
                "i2",
                102,
                "Issue 2",
                GitHubPropertyState.OPEN,
                now.minusDays(2),
                "http://issue2",
                null,
                null,
                null,
                null,
                emptyProjectItems);

        GitHubNodeDTO<IssueDTO> subIssueNode = new GitHubNodeDTO<>();
        subIssueNode.setNode(dto2);
        GitHubEdgesDTO<IssueDTO> subIssuesEdge = new GitHubEdgesDTO<>();
        subIssuesEdge.setEdges(List.of(subIssueNode));

        dtoSub = new IssueDTO(
                "i4",
                104,
                "Sub Issue Parent",
                GitHubPropertyState.OPEN,
                now.minusDays(4),
                "http://issue4",
                null,
                null,
                null,
                subIssuesEdge,
                emptyProjectItems);
    }

    @Test
    public void injectIssues_skips_whenCountsEqual() throws GitHubClientException, IssueInjectionException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueCount()).thenReturn(2);
        when(issueRepository.count()).thenReturn(2L);

        issueService.injectIssues();

        verify(gitHubClient, never()).getIssues();
        verify(issueRepository, never()).saveAll(anySet());
        verify(issueLabelRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectIssues_savesAllAndHandlesTypeMilestoneAndLabels()
            throws GitHubClientException, IssueInjectionException {
        Set<IssueDTO> DTOs = Set.of(dto1, dto2);

        when(mapper.toEntity(eq(dto1), eq(Issue.class))).thenReturn(issue1);
        when(mapper.toEntity(eq(dto2), eq(Issue.class))).thenReturn(issue2);

        Map<String, Milestone> milestones = Map.of("m1", milestone);
        Map<String, IssueType> issueTypes = Map.of("it1", issueType);

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueCount()).thenReturn(4);
        when(issueRepository.count()).thenReturn(2L);
        when(gitHubClient.getIssues()).thenReturn(DTOs);
        when(milestoneService.getAllMilestonesMap()).thenReturn(milestones);
        when(issueTypeService.getAllIssueTypesMap()).thenReturn(issueTypes);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        Label label = new Label();
        label.setId("l1");
        label.setName("bug");
        label.setColor("red");
        label.setDescription("desc");
        Map<String, Label> labelMap = Map.of("l1", label);
        when(labelService.getAllLabelsMap()).thenReturn(labelMap);
        when(issueLabelRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        issueService.injectIssues();

        verify(issueRepository, atLeastOnce()).saveAll(anySet());
        verify(issueLabelRepository, atLeastOnce()).saveAll(anySet());
        assertEquals(milestone, issue1.getMilestone());
    }

    @Test
    public void injectIssues_mapsPriorityAndPointsFromProjectItems() throws Exception {
        when(issuePriorityService.getAllIssuePrioritiesMap()).thenReturn(Collections.emptyMap());
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueCount()).thenReturn(5);
        when(issueRepository.count()).thenReturn(2L);
        when(gitHubClient.getIssues()).thenReturn(Set.of(dto1));
        when(mapper.toEntity(eq(dto1), eq(Issue.class))).thenReturn(issue1);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        issueService.injectIssues();

        verify(mapper).toEntity(eq(dto1), eq(Issue.class));
        assertEquals("High", issue1.getIssuePriority().getName());
        assertEquals(13.0, issue1.getPoints());
    }

    @Test
    public void injectIssues_handlesMissingPriorityMappingGracefully() throws Exception {
        when(issuePriorityService.getAllIssuePrioritiesMap()).thenReturn(Collections.emptyMap());
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueCount()).thenReturn(5);
        when(issueRepository.count()).thenReturn(2L);
        when(gitHubClient.getIssues()).thenReturn(Set.of(dtoSub));
        when(mapper.toEntity(eq(dtoSub), eq(Issue.class))).thenReturn(issueSub);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        assertDoesNotThrow(() -> issueService.injectIssues());
    }

    @Test
    public void injectIssues_handlesFieldValuesWithNullNode() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueCount()).thenReturn(5);
        when(issueRepository.count()).thenReturn(2L);
        when(gitHubClient.getIssues()).thenReturn(Set.of(dto1));
        when(mapper.toEntity(eq(dto1), eq(Issue.class))).thenReturn(issue1);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        assertDoesNotThrow(() -> issueService.injectIssues());
    }

    @Test
    public void injectIssues_assignsSubIssues() throws GitHubClientException, IssueInjectionException {
        Set<IssueDTO> DTOs = Set.of(dtoSub, dto2);

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueCount()).thenReturn(5);
        when(issueRepository.count()).thenReturn(2L);
        when(gitHubClient.getIssues()).thenReturn(DTOs);
        when(mapper.toEntity(eq(dtoSub), eq(Issue.class))).thenReturn(issueSub);
        when(mapper.toEntity(eq(dto2), eq(Issue.class))).thenReturn(issue2);
        when(milestoneService.getAllMilestonesMap()).thenReturn(Collections.emptyMap());
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));
        when(labelService.getAllLabelsMap()).thenReturn(Collections.emptyMap());

        issueService.injectIssues();

        verify(issueRepository, atLeastOnce()).saveAll(anySet());
        verify(issueLabelRepository, never()).saveAll(anySet());
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
        ir1.setClosedAt(now);
        IssueResponse ir2 = new IssueResponse();
        ir2.setId("i2");
        ir2.setClosedAt(now);

        when(mapper.toDTO(eq(issue1), eq(IssueResponse.class))).thenReturn(ir1);
        when(mapper.toDTO(eq(issue2), eq(IssueResponse.class))).thenReturn(ir2);
        when(labelService.getLabelsByIssueId(anyString())).thenReturn(Set.of(label));
        when(mapper.toDTO(eq(label), eq(LabelResponse.class))).thenReturn(lr);

        when(mapper.toDTO(any(IssueType.class), eq(IssueTypeResponse.class)))
                .thenReturn(new IssueTypeResponse("it1", "ImportantissueType1", "description1", "purple"));
        when(mapper.toDTO(any(IssuePriority.class), eq(IssuePriorityResponse.class)))
                .thenReturn(new IssuePriorityResponse("ip1", "High", "High priority issue", "red"));

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
        when(milestoneService.checkIfMilestoneExists("notfound"))
                .thenThrow(new MilestoneNotFoundException("Not found", null));
        assertThrows(MilestoneNotFoundException.class, () -> issueService.getIssuesByMilestoneId("notfound"));
    }

    @Test
    public void getIssuesByTimespan_mapsSubIssuesRecursively() {
        Issue parent = new Issue();
        parent.setId("parent");
        parent.setTitle("Parent");

        Issue child = new Issue();
        child.setId("child");
        child.setTitle("Child");
        child.setParentIssue(parent);

        Issue grandChild = new Issue();
        grandChild.setId("grandchild");
        grandChild.setTitle("Grandchild");
        grandChild.setParentIssue(child);

        when(issueRepository.findAllByClosedAtBetween(any(), any())).thenReturn(Set.of(parent));
        when(issueRepository.findAllByParentIssue_Id("parent")).thenReturn(Set.of(child));
        when(issueRepository.findAllByParentIssue_Id("child")).thenReturn(Set.of(grandChild));
        when(issueRepository.findAllByParentIssue_Id("grandchild")).thenReturn(Collections.emptySet());

        IssueResponse parentResp = new IssueResponse();
        parentResp.setId("parent");
        parentResp.setTitle("Parent");
        IssueResponse childResp = new IssueResponse();
        childResp.setId("child");
        childResp.setTitle("Child");
        IssueResponse grandChildResp = new IssueResponse();
        grandChildResp.setId("grandchild");
        grandChildResp.setTitle("Grandchild");
        when(mapper.toDTO(eq(parent), eq(IssueResponse.class))).thenReturn(parentResp);
        when(mapper.toDTO(eq(child), eq(IssueResponse.class))).thenReturn(childResp);
        when(mapper.toDTO(eq(grandChild), eq(IssueResponse.class))).thenReturn(grandChildResp);

        when(labelService.getLabelsByIssueId(anyString())).thenReturn(Collections.emptySet());

        Set<IssueResponse> responses = issueService.getIssuesByTimespan(
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1));

        assertEquals(1, responses.size());
        IssueResponse respParent = responses.iterator().next();
        assertEquals("parent", respParent.getId());
        assertNotNull(respParent.getSubIssues());
        assertEquals(1, respParent.getSubIssues().size());

        IssueResponse respChild = respParent.getSubIssues().iterator().next();
        assertEquals("child", respChild.getId());
        assertNotNull(respChild.getSubIssues());
        assertEquals(1, respChild.getSubIssues().size());

        IssueResponse respGrandChild = respChild.getSubIssues().iterator().next();
        assertEquals("grandchild", respGrandChild.getId());
        assertTrue(respGrandChild.getSubIssues() == null
                || respGrandChild.getSubIssues().isEmpty());
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
