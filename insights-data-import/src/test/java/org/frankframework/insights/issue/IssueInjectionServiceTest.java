package org.frankframework.insights.issue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.businessvalue.BusinessValue;
import org.frankframework.insights.common.client.graphql.GraphQLNodeDTO;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.enums.GitHubPropertyState;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.graphql.GitHubEdgesDTO;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
import org.frankframework.insights.github.graphql.GitHubIssueProjectItemDTO;
import org.frankframework.insights.issueprojects.IssuePriority;
import org.frankframework.insights.issueprojects.IssueProjectItemsInjectionService;
import org.frankframework.insights.issuetype.IssueType;
import org.frankframework.insights.issuetype.IssueTypeInjectionService;
import org.frankframework.insights.label.*;
import org.frankframework.insights.milestone.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssueInjectionServiceTest {

    @Mock
    private GitHubGraphQLClient gitHubGraphQLClient;

    @Mock
    private Mapper mapper;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @Mock
    private MilestoneInjectionService milestoneInjectionService;

    @Mock
    private IssueTypeInjectionService issueTypeInjectionService;

    @Mock
    private LabelInjectionService labelInjectionService;

    @Mock
    private IssueProjectItemsInjectionService issueProjectItemsInjectionService;

    @InjectMocks
    private IssueInjectionService issueInjectionService;

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

        GraphQLNodeDTO<LabelDTO> labelNode = new GraphQLNodeDTO<>(labelDTO);
        List<GraphQLNodeDTO<LabelDTO>> labelNodeList = List.of(labelNode);
        GitHubEdgesDTO<LabelDTO> labelEdges = new GitHubEdgesDTO<>(labelNodeList);

        GitHubEdgesDTO<GitHubIssueProjectItemDTO> emptyProjectItems = new GitHubEdgesDTO<>(Collections.emptyList());

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

        GraphQLNodeDTO<IssueDTO> subIssueNode = new GraphQLNodeDTO<>(dto2);
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
            throws GitHubGraphQLClientException, IssueInjectionException {
        Set<IssueDTO> dtos = Set.of(dto1, dto2);

        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(mapper.toEntity(dto1, Issue.class)).thenReturn(issue1);
        when(mapper.toEntity(dto2, Issue.class)).thenReturn(issue2);

        Map<String, Milestone> milestones = Map.of("m1", milestone);
        Map<String, IssueType> issueTypes = Map.of("it1", issueType);

        when(gitHubGraphQLClient.getIssues()).thenReturn(dtos);
        when(milestoneInjectionService.getAllMilestonesMap()).thenReturn(milestones);
        when(issueTypeInjectionService.getAllIssueTypesMap()).thenReturn(issueTypes);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        Label label = new Label();
        label.setId("l1");
        label.setName("bug");
        label.setColor("red");
        label.setDescription("desc");
        Map<String, Label> labelMap = Map.of("l1", label);
        when(labelInjectionService.getAllLabelsMap()).thenReturn(labelMap);
        when(issueLabelRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        issueInjectionService.injectIssues();

        verify(issueRepository, atLeastOnce()).saveAll(anySet());
        verify(issueLabelRepository, atLeastOnce()).saveAll(anySet());
        assertEquals(milestone, issue1.getMilestone());
    }

    @Test
    public void injectIssues_mapsPriorityAndPointsFromProjectItems()
            throws GitHubGraphQLClientException, IssueInjectionException {
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(issueProjectItemsInjectionService.getAllIssuePrioritiesMap()).thenReturn(Collections.emptyMap());
        when(gitHubGraphQLClient.getIssues()).thenReturn(Set.of(dto1));
        when(mapper.toEntity(dto1, Issue.class)).thenReturn(issue1);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        issueInjectionService.injectIssues();

        verify(mapper).toEntity(dto1, Issue.class);
        assertEquals("High", issue1.getIssuePriority().getName());
        assertEquals(13.0, issue1.getPoints());
    }

    @Test
    public void injectIssues_handlesMissingPriorityMappingGracefully() throws GitHubGraphQLClientException {
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(issueProjectItemsInjectionService.getAllIssuePrioritiesMap()).thenReturn(Collections.emptyMap());
        when(gitHubGraphQLClient.getIssues()).thenReturn(Set.of(dtoSub));
        when(mapper.toEntity(dtoSub, Issue.class)).thenReturn(issueSub);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        assertDoesNotThrow(() -> issueInjectionService.injectIssues());
    }

    @Test
    public void injectIssues_handlesFieldValuesWithNullNode() throws GitHubGraphQLClientException {
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(gitHubGraphQLClient.getIssues()).thenReturn(Set.of(dto1));
        when(mapper.toEntity(dto1, Issue.class)).thenReturn(issue1);
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        assertDoesNotThrow(() -> issueInjectionService.injectIssues());
    }

    @Test
    public void injectIssues_assignsSubIssues() throws GitHubGraphQLClientException, IssueInjectionException {
        Set<IssueDTO> dtos = Set.of(dtoSub, dto2);

        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(gitHubGraphQLClient.getIssues()).thenReturn(dtos);
        when(mapper.toEntity(dtoSub, Issue.class)).thenReturn(issueSub);
        when(mapper.toEntity(dto2, Issue.class)).thenReturn(issue2);
        when(milestoneInjectionService.getAllMilestonesMap()).thenReturn(Collections.emptyMap());
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));
        when(labelInjectionService.getAllLabelsMap()).thenReturn(Collections.emptyMap());

        issueInjectionService.injectIssues();

        verify(issueRepository, atLeastOnce()).saveAll(anySet());
        verify(issueLabelRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectIssues_catchesAndWrapsException() throws GitHubGraphQLClientException {
        when(gitHubGraphQLClient.getIssues()).thenThrow(new GitHubGraphQLClientException("fail", null));
        assertThrows(IssueInjectionException.class, () -> issueInjectionService.injectIssues());
    }

    @Test
    public void getAllIssuesMap_returnsMap() {
        when(issueRepository.findAll()).thenReturn(List.of(issue1, issue2));
        Map<String, Issue> result = issueInjectionService.getAllIssuesMap();
        assertEquals(2, result.size());
        assertEquals(issue1, result.get("i1"));
        assertEquals(issue2, result.get("i2"));
    }

    @Test
    public void injectIssues_preservesExistingBusinessValueLink() throws Exception {
        BusinessValue bv = new BusinessValue();
        bv.setTitle("My BV");

        Issue dbIssue1 = new Issue();
        dbIssue1.setId("i1");
        dbIssue1.setBusinessValue(bv);

        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(gitHubGraphQLClient.getIssues()).thenReturn(Set.of(dto1));
        when(mapper.toEntity(dto1, Issue.class)).thenReturn(issue1);
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(Set.of("i1")))
                .thenReturn(List.of(dbIssue1));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        issueInjectionService.injectIssues();

        assertEquals(bv, issue1.getBusinessValue());
    }

    @Test
    public void injectIssues_doesNotSetBusinessValueForNewIssues() throws Exception {
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(gitHubGraphQLClient.getIssues()).thenReturn(Set.of(dto2));
        when(mapper.toEntity(dto2, Issue.class)).thenReturn(issue2);
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(Set.of("i2")))
                .thenReturn(Collections.emptyList());
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        issueInjectionService.injectIssues();

        assertNull(issue2.getBusinessValue());
    }

    @Test
    public void injectIssues_preservesLinksForSomeIssuesButNotOthers() throws Exception {
        BusinessValue bv = new BusinessValue();
        bv.setTitle("BV for issue1");

        Issue dbIssue1 = new Issue();
        dbIssue1.setId("i1");
        dbIssue1.setBusinessValue(bv);

        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(gitHubGraphQLClient.getIssues()).thenReturn(Set.of(dto1, dto2));
        when(mapper.toEntity(dto1, Issue.class)).thenReturn(issue1);
        when(mapper.toEntity(dto2, Issue.class)).thenReturn(issue2);
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(Set.of("i1", "i2")))
                .thenReturn(List.of(dbIssue1));
        when(milestoneInjectionService.getAllMilestonesMap()).thenReturn(Collections.emptyMap());
        when(issueTypeInjectionService.getAllIssueTypesMap()).thenReturn(Collections.emptyMap());
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));
        when(labelInjectionService.getAllLabelsMap()).thenReturn(Collections.emptyMap());

        issueInjectionService.injectIssues();

        assertEquals(bv, issue1.getBusinessValue());
        assertNull(issue2.getBusinessValue());
    }

    @Test
    public void injectIssues_handlesNoIssuesWithBusinessValues() throws Exception {
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(gitHubGraphQLClient.getIssues()).thenReturn(Set.of(dto2));
        when(mapper.toEntity(dto2, Issue.class)).thenReturn(issue2);
        when(issueRepository.findAllByIdInAndBusinessValueIsNotNull(anySet())).thenReturn(Collections.emptyList());
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        assertDoesNotThrow(() -> issueInjectionService.injectIssues());
        assertNull(issue2.getBusinessValue());
    }
}
