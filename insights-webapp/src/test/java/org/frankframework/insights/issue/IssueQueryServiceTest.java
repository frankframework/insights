package org.frankframework.insights.issue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.enums.GitHubPropertyState;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.issueprojects.IssuePriority;
import org.frankframework.insights.issueprojects.IssuePriorityResponse;
import org.frankframework.insights.issuetype.IssueType;
import org.frankframework.insights.issuetype.IssueTypeResponse;
import org.frankframework.insights.label.*;
import org.frankframework.insights.milestone.*;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseQueryService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssueQueryServiceTest {

    @Mock
    private Mapper mapper;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @Mock
    private MilestoneQueryService milestoneQueryService;

    @Mock
    private LabelQueryService labelQueryService;

    @Mock
    private ReleaseQueryService releaseQueryService;

    @InjectMocks
    private IssueQueryService issueQueryService;

    private Issue issue1, issueSub;
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
    }

    @Test
    public void getIssuesByReleaseId_returnsResponsesWithLabels() throws ReleaseNotFoundException {
        Release release = mock(Release.class);
        when(release.getId()).thenReturn("rel123");
        when(releaseQueryService.checkIfReleaseExists("rel123")).thenReturn(release);
        when(issueRepository.findIssuesByReleaseId("rel123")).thenReturn(Set.of(issue1));

        Label label = new Label();
        label.setId("l1");
        label.setColor("RED");
        LabelResponse lr = new LabelResponse("l1", "bug", "desc", "RED");

        IssueLabel issueLabel = new IssueLabel(issue1, label);
        when(issueLabelRepository.findAllByIssue_IdIn(any())).thenReturn(Set.of(issueLabel));
        when(labelQueryService.isLabelIncluded(any(Label.class))).thenReturn(true);

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

        Set<IssueResponse> resp = issueQueryService.getIssuesByReleaseId("rel123");
        assertEquals(1, resp.size());
        assertEquals("i1", resp.iterator().next().getId());
    }

    @Test
    public void getIssuesByReleaseId_throwsIfNotFound() throws Exception {
        when(releaseQueryService.checkIfReleaseExists("notfound"))
                .thenThrow(new ReleaseNotFoundException("Not found", null));
        assertThrows(ReleaseNotFoundException.class, () -> issueQueryService.getIssuesByReleaseId("notfound"));
    }

    @Test
    public void getRootIssuesByReleaseId_filtersOutSubIssues() throws ReleaseNotFoundException {
        Release release = mock(Release.class);
        when(release.getId()).thenReturn("rel123");
        when(releaseQueryService.checkIfReleaseExists("rel123")).thenReturn(release);
        when(issueRepository.findIssuesByReleaseId("rel123")).thenReturn(Set.of(issue1, issueSub));

        Set<Issue> roots = issueQueryService.getRootIssuesByReleaseId("rel123");

        assertEquals(1, roots.size());
        assertEquals("i1", roots.iterator().next().getId());
    }

    @Test
    public void getIssuesByMilestoneId_returnsResponsesWithLabels() throws MilestoneNotFoundException {
        when(milestoneQueryService.checkIfMilestoneExists("m1")).thenReturn(milestone);
        when(issueRepository.findDistinctByMilestoneId("m1")).thenReturn(Set.of(issue1));

        Label label = new Label();
        label.setId("l1");
        label.setColor("RED");
        LabelResponse lr = new LabelResponse("l1", "bug", "desc", "RED");

        IssueLabel issueLabel = new IssueLabel(issue1, label);
        when(issueLabelRepository.findAllByIssue_IdIn(any())).thenReturn(Set.of(issueLabel));
        when(labelQueryService.isLabelIncluded(any(Label.class))).thenReturn(true);

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

        Set<IssueResponse> resp = issueQueryService.getIssuesByMilestoneId("m1");
        assertEquals(1, resp.size());
        assertEquals("i1", resp.iterator().next().getId());
    }

    @Test
    public void getIssuesByMilestoneId_throwsIfNotFound() throws MilestoneNotFoundException {
        when(milestoneQueryService.checkIfMilestoneExists("notfound"))
                .thenThrow(new MilestoneNotFoundException("Not found", null));
        assertThrows(MilestoneNotFoundException.class, () -> issueQueryService.getIssuesByMilestoneId("notfound"));
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

        Set<IssueResponse> result = issueQueryService.getFutureEpicIssues();

        verify(issueRepository).findIssuesByIssueTypeNameAndMilestoneIsNull("Epic");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("epic1", result.iterator().next().getId());
    }

    @Test
    public void shouldReturnEmptySetWhenNoFutureEpicsAreFound() {
        when(issueRepository.findIssuesByIssueTypeNameAndMilestoneIsNull("Epic"))
                .thenReturn(Collections.emptySet());

        Set<IssueResponse> result = issueQueryService.getFutureEpicIssues();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(issueRepository).findIssuesByIssueTypeNameAndMilestoneIsNull("Epic");
    }
}
