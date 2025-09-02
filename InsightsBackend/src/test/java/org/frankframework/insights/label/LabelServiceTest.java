package org.frankframework.insights.label;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabelServiceTest {

    @Mock
    private GitHubRepositoryStatisticsService statisticsService;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper mapper;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @Mock
    private ReleaseService releaseService;

    @Mock
    private GitHubRepositoryStatisticsDTO statisticsDTO;

    @Captor
    private ArgumentCaptor<LinkedHashSet<Label>> labelSetCaptor;

    private LabelService labelService;

    @BeforeEach
	public void setUp() {
        List<String> priorityColors = List.of("D73A4A", "B60205");
        List<String> ignoredColors = List.of("EEEEEE", "FBCA04");

        GitHubProperties gitHubProperties = mock(GitHubProperties.class);
        when(gitHubProperties.getPriorityLabels()).thenReturn(priorityColors);
        when(gitHubProperties.getIgnoredLabels()).thenReturn(ignoredColors);

        labelService = new LabelService(
                statisticsService,
                gitHubClient,
                mapper,
                labelRepository,
                issueLabelRepository,
                gitHubProperties,
                releaseService);
    }

    @Test
	public void shouldSkipIfLabelCountsAreEqual() throws LabelInjectionException, GitHubClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(5);
        when(labelRepository.count()).thenReturn(5L);

        labelService.injectLabels();

        verify(gitHubClient, never()).getLabels();
        verify(labelRepository, never()).saveAll(anySet());
    }

    @Test
	public void shouldSaveAllLabelsWhenCountsDiffer() throws Exception {
        LabelDTO dto1 = new LabelDTO("l1", "bug", "Of type bug", "D73A4A");
        LabelDTO dto2 = new LabelDTO("l2", "feature", "A new feature", "007BFF");
        Set<LabelDTO> dtos = Set.of(dto1, dto2);
        Set<Label> entities = Set.of(createLabel("l1", "bug", "Of type bug", "D73A4A"));

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(10);
        when(labelRepository.count()).thenReturn(1L);
        when(gitHubClient.getLabels()).thenReturn(dtos);
        when(mapper.toEntity(dtos, Label.class)).thenReturn(entities);

        labelService.injectLabels();

        verify(labelRepository).saveAll(entities);
    }

    @Test
	public void shouldThrowLabelInjectionException_whenClientFails() throws GitHubClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(4);
        when(labelRepository.count()).thenReturn(1L);
        when(gitHubClient.getLabels()).thenThrow(new GitHubClientException("API fetch failed", null));

        assertThrows(LabelInjectionException.class, () -> labelService.injectLabels());
    }

    @Test
	public void shouldThrowLabelInjectionException_whenMappingFails() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(10);
        when(labelRepository.count()).thenReturn(0L);
        when(gitHubClient.getLabels()).thenReturn(Collections.emptySet());
        when(mapper.toEntity(anySet(), eq(Label.class))).thenThrow(new MappingException("Mapping failed", null));

        assertThrows(LabelInjectionException.class, () -> labelService.injectLabels());
    }

    @Test
	public void shouldCorrectlyFilterSortAndPrioritizeLabels() throws Exception {
        Release release = new Release();
        release.setId("r1");
        when(releaseService.checkIfReleaseExists("r1")).thenReturn(release);
        Label labelPriority = createLabel("l1", "bug", "High priority", "D73A4A");
        Label labelPopular = createLabel("l2", "feature", "Popular feature", "007BFF");
        Label labelIgnored = createLabel("l3", "wontfix", "Ignored", "EEEEEE");
        Label labelDocs = createLabel("l4", "docs", "Documentation", "1D76DB");

        List<Label> releaseLabels = Stream.of(
                        Collections.nCopies(5, labelPopular),
                        Collections.nCopies(3, labelDocs),
                        Collections.nCopies(2, labelPriority),
                        Collections.nCopies(10, labelIgnored))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        when(labelRepository.findLabelsByReleaseId("r1")).thenReturn(releaseLabels);

        labelService.getHighlightsByReleaseId("r1");

        verify(mapper).toDTO(labelSetCaptor.capture(), eq(LabelResponse.class));
        List<Label> highlights = new ArrayList<>(labelSetCaptor.getValue());

        assertEquals(3, highlights.size(), "Should contain 3 labels after filtering.");
        assertEquals("l1", highlights.get(0).getId(), "Priority label (bug) should be first.");
        assertEquals("l2", highlights.get(1).getId(), "Most popular non-priority (feature) should be second.");
        assertEquals("l4", highlights.get(2).getId(), "Less popular non-priority (docs) should be third.");
    }

    @Test
	public void shouldCorrectlySortAmongMultiplePriorityLabelsByPopularity() throws Exception {
        Release release = new Release();
        release.setId("r_multi_priority");
        when(releaseService.checkIfReleaseExists("r_multi_priority")).thenReturn(release);
        Label priority1 = createLabel("p1", "critical-bug", "P1", "D73A4A");
        Label priority2 = createLabel("p2", "security-vuln", "P2", "B60205");

        List<Label> releaseLabels = Stream.of(
                        Collections.nCopies(2, priority1), // Less popular priority
                        Collections.nCopies(5, priority2) // More popular priority
                        )
                .flatMap(List::stream)
                .collect(Collectors.toList());
        when(labelRepository.findLabelsByReleaseId("r_multi_priority")).thenReturn(releaseLabels);

        labelService.getHighlightsByReleaseId("r_multi_priority");

        verify(mapper).toDTO(labelSetCaptor.capture(), eq(LabelResponse.class));
        List<Label> highlights = new ArrayList<>(labelSetCaptor.getValue());

        assertEquals(2, highlights.size());
        assertEquals("p2", highlights.get(0).getId(), "More popular priority label should be first.");
        assertEquals("p1", highlights.get(1).getId(), "Less popular priority label should be second.");
    }

    @Test
	public void shouldHandleCaseInsensitiveColorsForFiltering() throws Exception {
        Release release = new Release();
        release.setId("r2");
        when(releaseService.checkIfReleaseExists("r2")).thenReturn(release);
        Label priorityLower = createLabel("p1", "bugfix", "", "d73a4a"); // lowercase of D73A4A
        Label ignoredMixed = createLabel("i1", "duplicate", "", "fBcA04"); // mixed-case of FBCA04
        Label validLabel = createLabel("v1", "Valid", "", "123456");

        when(labelRepository.findLabelsByReleaseId("r2")).thenReturn(List.of(ignoredMixed, validLabel, priorityLower));

        labelService.getHighlightsByReleaseId("r2");

        verify(mapper).toDTO(labelSetCaptor.capture(), eq(LabelResponse.class));
        List<Label> highlights = new ArrayList<>(labelSetCaptor.getValue());

        assertEquals(2, highlights.size(), "Should contain 2 labels after filtering.");
        assertEquals("p1", highlights.get(0).getId(), "Priority label with lowercase color should be first.");
        assertEquals("v1", highlights.get(1).getId(), "The valid label should be second.");
    }

    @Test
	public void shouldLimitToMaxHighlightedLabels() throws Exception {
        Release release = new Release();
        release.setId("max");
        when(releaseService.checkIfReleaseExists("max")).thenReturn(release);
        List<Label> manyLabels = IntStream.range(0, 20)
                .mapToObj(i -> createLabel("l" + i, "n" + i, "d" + i, "007BFF"))
                .collect(Collectors.toList());
        when(labelRepository.findLabelsByReleaseId("max")).thenReturn(manyLabels);

        labelService.getHighlightsByReleaseId("max");

        verify(mapper).toDTO(labelSetCaptor.capture(), eq(LabelResponse.class));
        assertEquals(15, labelSetCaptor.getValue().size(), "Result should be limited to 15 labels.");
    }

    @Test
	public void shouldReturnEmptySet_whenReleaseHasNoLabels() throws Exception {
        Release release = new Release();
        release.setId("relX");
        when(releaseService.checkIfReleaseExists("relX")).thenReturn(release);
        when(labelRepository.findLabelsByReleaseId("relX")).thenReturn(Collections.emptyList());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("relX");

        assertTrue(result.isEmpty());
        verify(mapper, never()).toDTO(anySet(), eq(LabelResponse.class));
    }

    @Test
	public void shouldReturnEmptySet_ifAllLabelsAreIgnored() throws Exception {
        Release release = new Release();
        release.setId("r_ignored");
        when(releaseService.checkIfReleaseExists("r_ignored")).thenReturn(release);
        Label ignored1 = createLabel("i1", "ignored1", "desc", "EEEEEE");
        Label ignored2 = createLabel("i2", "ignored2", "desc", "FBCA04");

        when(labelRepository.findLabelsByReleaseId("r_ignored")).thenReturn(List.of(ignored1, ignored2));
        when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenReturn(Collections.emptySet());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("r_ignored");
        assertTrue(result.isEmpty());
    }

    @Test
	public void shouldThrowReleaseNotFoundException() throws ReleaseNotFoundException {
        when(releaseService.checkIfReleaseExists("notfound"))
                .thenThrow(new ReleaseNotFoundException("Release not found", null));
        assertThrows(ReleaseNotFoundException.class, () -> labelService.getHighlightsByReleaseId("notfound"));
    }

    @Test
	public void shouldReturnAssociatedLabels() {
        Label label1 = createLabel("l1", "feature", "desc", "blue");
        Label label2 = createLabel("l2", "bug", "desc", "red");
        IssueLabel il1 = new IssueLabel();
        il1.setLabel(label1);
        IssueLabel il2 = new IssueLabel();
        il2.setLabel(label2);
        when(issueLabelRepository.findAllByIssue_Id("i1")).thenReturn(Set.of(il1, il2));

        Set<Label> result = labelService.getLabelsByIssueId("i1");

        assertEquals(2, result.size());
        assertTrue(result.contains(label1));
        assertTrue(result.contains(label2));
    }

    @Test
    public void shouldReturnEmptySet_whenNoLabelsFound() {
        when(issueLabelRepository.findAllByIssue_Id("i99")).thenReturn(Collections.emptySet());
        Set<Label> result = labelService.getLabelsByIssueId("i99");
        assertTrue(result.isEmpty());
    }

    private Label createLabel(String id, String name, String description, String color) {
        Label label = new Label();
        label.setId(id);
        label.setName(name);
        label.setDescription(description);
        label.setColor(color);
        return label;
    }
}
