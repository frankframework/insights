package org.frankframework.insights.label;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.helper.ReleaseIssueHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabelServiceTest {

    @Mock
    GitHubRepositoryStatisticsService statisticsService;

    @Mock
    GitHubClient gitHubClient;

    @Mock
    Mapper mapper;

    @Mock
    LabelRepository labelRepository;

    @Mock
    ReleaseIssueHelperService releaseIssueHelperService;

    @Mock
    IssueLabelRepository issueLabelRepository;

    @Mock
    GitHubRepositoryStatisticsDTO statisticsDTO;

    private LabelService labelService;

    private Label labelBug;
    private Label labelFeature;
    private IssueLabel issueLabelBug, issueLabelFeature, issueLabelChore;
    private LabelDTO labelDTO1, labelDTO2;

    @BeforeEach
    void setUp() {
        List<String> priorityColors = List.of("red");

        GitHubProperties gitHubProperties = mock(GitHubProperties.class);
        when(gitHubProperties.getPriorityLabels()).thenReturn(priorityColors);

        labelService = new LabelService(
                statisticsService,
                gitHubClient,
                mapper,
                labelRepository,
                releaseIssueHelperService,
                issueLabelRepository,
                gitHubProperties);

        labelBug = new Label();
        labelBug.setId(UUID.randomUUID().toString());
        labelBug.setName("bug");
        labelBug.setDescription("Of type bug");
        labelBug.setColor("red");

        labelFeature = new Label();
        labelFeature.setId(UUID.randomUUID().toString());
        labelFeature.setName("feature");
        labelFeature.setDescription("Feature");
        labelFeature.setColor("blue");

        Label labelChore = new Label();
        labelChore.setId(UUID.randomUUID().toString());
        labelChore.setName("chore");
        labelChore.setDescription("Chore");
        labelChore.setColor("yellow");

        issueLabelBug = new IssueLabel();
        issueLabelBug.setLabel(labelBug);

        issueLabelFeature = new IssueLabel();
        issueLabelFeature.setLabel(labelFeature);

        issueLabelChore = new IssueLabel();
        issueLabelChore.setLabel(labelChore);

        labelDTO1 = new LabelDTO();
        labelDTO1.id = "l1";
        labelDTO1.name = "bug";
        labelDTO1.color = "red";

        labelDTO2 = new LabelDTO();
        labelDTO2.id = "l2";
        labelDTO2.name = "feature";
        labelDTO2.color = "blue";
    }

    @Test
    public void injectLabels_shouldSkipIfCountsEqual() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(5);
        when(labelRepository.count()).thenReturn(5L);

        labelService.injectLabels();

        verify(gitHubClient, never()).getLabels();
        verify(labelRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectLabels_shouldSaveAllLabels() throws Exception {
        Set<LabelDTO> dtos = Set.of(labelDTO1, labelDTO2);
        Set<Label> entities = Set.of(labelBug, labelFeature);
        List<Label> saved = List.of(labelBug, labelFeature);

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(3);
        when(labelRepository.count()).thenReturn(2L);
        when(gitHubClient.getLabels()).thenReturn(dtos);
        when(mapper.toEntity(dtos, Label.class)).thenReturn(entities);
        when(labelRepository.saveAll(entities)).thenReturn(saved);

        labelService.injectLabels();

        verify(labelRepository).saveAll(entities);
    }

    @Test
    public void injectLabels_shouldThrowOnException() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(4);
        when(labelRepository.count()).thenReturn(1L);
        when(gitHubClient.getLabels()).thenThrow(new GitHubClientException("fail", null));

        assertThrows(LabelInjectionException.class, () -> labelService.injectLabels());
    }

    @Test
    public void getHighlightsByReleaseId_shouldReturnOnlyPriorityLabels() throws Exception {
        Issue issueA = new Issue();
        issueA.setId("iA");
        Issue issueB = new Issue();
        issueB.setId("iB");

        Set<Issue> issues = Set.of(issueA, issueB);

        when(releaseIssueHelperService.getIssuesByReleaseId("r1")).thenReturn(issues);
        when(issueLabelRepository.findAllByIssue_Id("iA")).thenReturn(Set.of(issueLabelBug, issueLabelFeature));
        when(issueLabelRepository.findAllByIssue_Id("iB")).thenReturn(Set.of(issueLabelFeature));

        Set<Label> expectedFiltered = Set.of(labelBug);
        Set<LabelResponse> responses = Set.of(new LabelResponse("l1", "bug", "desc", "red"));
        when(mapper.toDTO(expectedFiltered, LabelResponse.class)).thenReturn(responses);

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("r1");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(r -> "red".equals(r.color())));
        verify(mapper).toDTO(expectedFiltered, LabelResponse.class);
    }

    @Test
    public void getHighlightsByReleaseId_shouldReturnEmptyIfNoPriorityLabels() throws Exception {
        Issue issueA = new Issue();
        issueA.setId("iA");
        Set<Issue> issues = Set.of(issueA);

        when(releaseIssueHelperService.getIssuesByReleaseId("r1")).thenReturn(issues);
        when(issueLabelRepository.findAllByIssue_Id("iA")).thenReturn(Set.of(issueLabelFeature, issueLabelChore));
        when(mapper.toDTO(Collections.emptySet(), LabelResponse.class)).thenReturn(Collections.emptySet());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("r1");

        assertTrue(result.isEmpty());
    }

    @Test
    public void getHighlightsByReleaseId_shouldReturnEmptyIfNoIssuesOrLabels() throws Exception {
        when(releaseIssueHelperService.getIssuesByReleaseId(anyString())).thenReturn(Collections.emptySet());
        when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenReturn(Collections.emptySet());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("relX");
        assertTrue(result.isEmpty());
    }

    @Test
    public void getHighlightsByReleaseId_shouldThrowIfReleaseNotFound() throws Exception {
        when(releaseIssueHelperService.getIssuesByReleaseId("notfound"))
                .thenThrow(new ReleaseNotFoundException("Release not found", null));
        assertThrows(ReleaseNotFoundException.class, () -> labelService.getHighlightsByReleaseId("notfound"));
    }

    @Test
    public void getHighlightsByReleaseId_shouldThrowIfMappingFails() throws Exception {
        Issue issueA = new Issue();
        issueA.setId("iA");
        Set<Issue> issues = Set.of(issueA);

        when(releaseIssueHelperService.getIssuesByReleaseId("failMap")).thenReturn(issues);
        when(issueLabelRepository.findAllByIssue_Id("iA")).thenReturn(Set.of(issueLabelBug));
        when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenThrow(new MappingException("Mapping failed", null));

        assertThrows(MappingException.class, () -> labelService.getHighlightsByReleaseId("failMap"));
    }

    @Test
    public void getLabelsByIssueId_shouldReturnLabels() {
        when(issueLabelRepository.findAllByIssue_Id("i1")).thenReturn(Set.of(issueLabelBug, issueLabelFeature));
        Set<Label> result = labelService.getLabelsByIssueId("i1");
        assertEquals(2, result.size());
        assertTrue(result.contains(labelBug));
        assertTrue(result.contains(labelFeature));
    }

    @Test
    public void getLabelsByIssueId_shouldReturnEmptyIfNoLabels() {
        when(issueLabelRepository.findAllByIssue_Id("i99")).thenReturn(Set.of());
        Set<Label> result = labelService.getLabelsByIssueId("i99");
        assertTrue(result.isEmpty());
    }

    @Test
    public void saveLabels_andShouldLogInfo() throws Exception {
        Set<Label> labels = Set.of(labelBug, labelFeature);
        when(labelRepository.saveAll(labels)).thenReturn(List.of(labelBug, labelFeature));

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(1);
        when(labelRepository.count()).thenReturn(0L);
        when(gitHubClient.getLabels()).thenReturn(Set.of(labelDTO1, labelDTO2));
        when(mapper.toEntity(anySet(), eq(Label.class))).thenReturn(labels);

        labelService.injectLabels();

        verify(labelRepository).saveAll(labels);
    }
}
