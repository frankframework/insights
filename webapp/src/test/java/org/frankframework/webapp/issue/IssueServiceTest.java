package org.frankframework.webapp.issue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.shared.entity.Release;
import org.frankframework.webapp.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.webapp.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.webapp.common.mapper.Mapper;
import org.frankframework.webapp.github.*;
import org.frankframework.webapp.issuePriority.IssuePriority;
import org.frankframework.webapp.issuePriority.IssuePriorityResponse;
import org.frankframework.webapp.issuePriority.IssuePriorityService;
import org.frankframework.webapp.issuetype.IssueType;
import org.frankframework.webapp.issuetype.IssueTypeResponse;
import org.frankframework.webapp.issuetype.IssueTypeService;
import org.frankframework.webapp.label.*;
import org.frankframework.webapp.milestone.*;
import org.frankframework.webapp.release.ReleaseNotFoundException;
import org.frankframework.webapp.release.ReleaseService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssueServiceTest {

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
    private ReleaseService releaseService;

    @InjectMocks
    private IssueService issueService;

    private IssueDTO dto1, dto2, dtoSub;
    private Issue issue1, issue2, issueSub;
    private Milestone milestone;
    private IssueType issueType;

    @BeforeEach
    public void setup() {
        OffsetDateTime now = OffsetDateTime.now();

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

        issueSub = new Issue();
        issueSub.setId("i4");
        issueSub.setNumber(104);
        issueSub.setTitle("Sub Issue Parent");
        issueSub.setState(GitHubPropertyState.OPEN);
        issueSub.setUrl("http://issue4");

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
        issue1.setSubIssues(Set.of(issueSub));

        issue2 = new Issue();
        issue2.setId("i2");
        issue2.setNumber(102);
        issue2.setTitle("Issue 2");
        issue2.setState(GitHubPropertyState.OPEN);
        issue2.setUrl("http://issue2");
        issue2.setClosedAt(now.minusDays(2));

        LabelDTO labelDTO = new LabelDTO("l1", "bug", "desc", "red");

        GitHubNodeDTO<LabelDTO> labelNode = new GitHubNodeDTO<>(labelDTO);
        List<GitHubNodeDTO<LabelDTO>> labelNodeList = List.of(labelNode);
        GitHubEdgesDTO<LabelDTO> labelEdges = new GitHubEdgesDTO<>(labelNodeList);

        GitHubEdgesDTO<GitHubProjectItemDTO> emptyProjectItems = new GitHubEdgesDTO<>(Collections.emptyList());

        dto1 = new IssueDTO(
                "i1",
                101,
                "Issue 1",
                GitHubPropertyState.OPEN,
                now.minusDays(1),
                "http://issue1",
                labelEdges,
                new MilestoneDTO("m1", 1, "Milestone 1", "https//example.com", GitHubPropertyState.OPEN, null, 0, 0),
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

        GitHubNodeDTO<IssueDTO> subIssueNode = new GitHubNodeDTO<>(dto2);
        GitHubEdgesDTO<IssueDTO> subIssuesEdge = new GitHubEdgesDTO<>(List.of(subIssueNode));

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
    public void injectIssues_savesAllAndHandlesTypeMilestoneAndLabels()
            throws GitHubClientException, IssueInjectionException {
        Set<IssueDTO> DTOs = Set.of(dto1, dto2);

        when(mapper.toEntity(eq(dto1), eq(Issue.class))).thenReturn(issue1);
        when(mapper.toEntity(eq(dto2), eq(Issue.class))).thenReturn(issue2);

        Map<String, Milestone> milestones = Map.of("m1", milestone);
        Map<String, IssueType> issueTypes = Map.of("it1", issueType);

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
    public void injectIssues_mapsPriorityAndPointsFromProjectItems()
            throws GitHubClientException, IssueInjectionException {
        when(issuePriorityService.getAllIssuePrioritiesMap()).thenReturn(Collections.emptyMap());
        when(gitHubClient.getIssues()).thenReturn(Set.of(dto1));
        when(mapper.toEntity(eq(dto1), eq(Issue.class))).thenReturn(issue1);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        issueService.injectIssues();

        verify(mapper).toEntity(eq(dto1), eq(Issue.class));
        assertEquals("High", issue1.getIssuePriority().getName());
        assertEquals(13.0, issue1.getPoints());
    }

    @Test
    public void injectIssues_handlesMissingPriorityMappingGracefully() throws GitHubClientException {
        when(issuePriorityService.getAllIssuePrioritiesMap()).thenReturn(Collections.emptyMap());
        when(gitHubClient.getIssues()).thenReturn(Set.of(dtoSub));
        when(mapper.toEntity(eq(dtoSub), eq(Issue.class))).thenReturn(issueSub);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        assertDoesNotThrow(() -> issueService.injectIssues());
    }

    @Test
    public void injectIssues_handlesFieldValuesWithNullNode() throws GitHubClientException {
        when(gitHubClient.getIssues()).thenReturn(Set.of(dto1));
        when(mapper.toEntity(eq(dto1), eq(Issue.class))).thenReturn(issue1);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        assertDoesNotThrow(() -> issueService.injectIssues());
    }

    @Test
    public void injectIssues_assignsSubIssues() throws GitHubClientException, IssueInjectionException {
        Set<IssueDTO> DTOs = Set.of(dtoSub, dto2);

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
        when(gitHubClient.getIssues()).thenThrow(new GitHubClientException("fail", null));
        assertThrows(IssueInjectionException.class, () -> issueService.injectIssues());
    }

    @Test
    public void getIssuesByReleaseId_returnsResponsesWithLabels() throws ReleaseNotFoundException {
        Release release = mock(Release.class);
        when(release.getId()).thenReturn("rel123");
        when(releaseService.checkIfReleaseExists("rel123")).thenReturn(release);
        when(issueRepository.findIssuesByReleaseId("rel123")).thenReturn(Set.of(issue1));

        Label label = new Label();
        label.setId("l1");
        LabelResponse lr = new LabelResponse("l1", "bug", "desc", "red");

        IssueLabel issueLabel = new IssueLabel(issue1, label);
        when(issueLabelRepository.findAllByIssue_IdIn(any())).thenReturn(Set.of(issueLabel));

        when(mapper.toDTO(any(Issue.class), eq(IssueResponse.class))).thenAnswer(inv -> {
            Issue issue = inv.getArgument(0);
            IssueResponse ir = new IssueResponse();
            ir.setId(issue.getId());
            return ir;
        });
        when(mapper.toDTO(any(Label.class), eq(LabelResponse.class))).thenReturn(lr);
        when(mapper.toDTO(any(IssueType.class), eq(IssueTypeResponse.class)))
                .thenReturn(new IssueTypeResponse("it1", "ImportantissueType1", "description1", "purple"));
        when(mapper.toDTO(any(IssuePriority.class), eq(IssuePriorityResponse.class)))
                .thenReturn(new IssuePriorityResponse("ip1", "High", "High priority issue", "red"));

        Set<IssueResponse> resp = issueService.getIssuesByReleaseId("rel123");
        assertEquals(1, resp.size());
        assertEquals("i1", resp.iterator().next().getId());
    }

    @Test
    public void getIssuesByReleaseId_throwsIfNotFound() throws Exception {
        when(releaseService.checkIfReleaseExists("notfound"))
                .thenThrow(new ReleaseNotFoundException("Not found", null));
        assertThrows(ReleaseNotFoundException.class, () -> issueService.getIssuesByReleaseId("notfound"));
    }

    @Test
    public void getIssuesByMilestoneId_returnsResponsesWithLabels() throws MilestoneNotFoundException {
        when(milestoneService.checkIfMilestoneExists("m1")).thenReturn(milestone);
        when(issueRepository.findDistinctByMilestoneId("m1")).thenReturn(Set.of(issue1));

        Label label = new Label();
        label.setId("l1");
        LabelResponse lr = new LabelResponse("l1", "bug", "desc", "red");

        IssueLabel issueLabel = new IssueLabel(issue1, label);
        when(issueLabelRepository.findAllByIssue_IdIn(any())).thenReturn(Set.of(issueLabel));

        when(mapper.toDTO(any(Issue.class), eq(IssueResponse.class))).thenAnswer(inv -> {
            Issue issue = inv.getArgument(0);
            IssueResponse ir = new IssueResponse();
            ir.setId(issue.getId());
            return ir;
        });
        when(mapper.toDTO(any(Label.class), eq(LabelResponse.class))).thenReturn(lr);
        when(mapper.toDTO(any(IssueType.class), eq(IssueTypeResponse.class)))
                .thenReturn(new IssueTypeResponse("it1", "ImportantissueType1", "description1", "purple"));
        when(mapper.toDTO(any(IssuePriority.class), eq(IssuePriorityResponse.class)))
                .thenReturn(new IssuePriorityResponse("ip1", "High", "High priority issue", "red"));

        Set<IssueResponse> resp = issueService.getIssuesByMilestoneId("m1");
        assertEquals(1, resp.size());
        assertEquals("i1", resp.iterator().next().getId());
    }

    @Test
    public void getIssuesByMilestoneId_throwsIfNotFound() throws MilestoneNotFoundException {
        when(milestoneService.checkIfMilestoneExists("notfound"))
                .thenThrow(new MilestoneNotFoundException("Not found", null));
        assertThrows(MilestoneNotFoundException.class, () -> issueService.getIssuesByMilestoneId("notfound"));
    }

    @Test
    public void getFutureEpicIssues_shouldReturnUnassignedEpics() {
        Issue unassignedEpic = new Issue();
        unassignedEpic.setId("epic1");
        unassignedEpic.setMilestone(null);

        when(issueRepository.findIssuesByIssueTypeNameAndMilestoneIsNull("Epic"))
                .thenReturn(Set.of(unassignedEpic));

        IssueResponse epicResponse = new IssueResponse();
        epicResponse.setId("epic1");
        when(mapper.toDTO(unassignedEpic, IssueResponse.class)).thenReturn(epicResponse);

        Set<IssueResponse> result = issueService.getFutureEpicIssues();

        verify(issueRepository).findIssuesByIssueTypeNameAndMilestoneIsNull("Epic");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("epic1", result.iterator().next().getId());
    }

    @Test
    public void shouldReturnEmptySetWhenNoFutureEpicsAreFound() {
        when(issueRepository.findIssuesByIssueTypeNameAndMilestoneIsNull("Epic"))
                .thenReturn(Collections.emptySet());

        Set<IssueResponse> result = issueService.getFutureEpicIssues();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(issueRepository).findIssuesByIssueTypeNameAndMilestoneIsNull("Epic");
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
